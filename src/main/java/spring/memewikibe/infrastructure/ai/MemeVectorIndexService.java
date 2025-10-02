package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.memewikibe.domain.meme.Meme;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemeVectorIndexService {

    private final KoreanEmbeddingService embeddingService;
    // Optional beans for hybrid search and heavy rerank
    private final java.util.Optional<MemeVectorIndexService.KeywordSearchService> keywordSearchService;
    private final java.util.Optional<MemeVectorIndexService.Reranker> heavyReranker;
    private final Optional<QueryRewriter> queryRewriter; // [추가] QueryRewriter 주입

    // Simple in-memory cache (TTL) for query results
    private final SearchCache cache = new SearchCache(java.time.Duration.ofSeconds(60));

    @Value("${PINECONE_API_KEY:}")
    private String apiKey;

    // Optional: if not provided, service will be no-op for network calls.
    // Example: https://your-index-host.svc.us-east-1-aws.pinecone.io
    @Value("${PINECONE_INDEX_HOST:}")
    private String indexHost;

    @Value("${PINECONE_INDEX_NAME:meme-recommendations}")
    private String indexName;

    @Value("${PINECONE_ENVIRONMENT:}")
    private String environment; // e.g., us-east-1-aws

    @Value("${PINECONE_NAMESPACE:default}")
    private String namespace;

    // Optional: allow explicitly configuring index dimension via env
    @Value("${PINECONE_INDEX_DIMENSION:0}")
    private int configuredIndexDimension;

    // Resolved Pinecone index dimension (from describe). 0 if unknown.
    private volatile int resolvedIndexDimension = 0;

    private final HttpClient http = HttpClient.newHttpClient();

    public void index(Meme meme) {
        upsertVectors(List.of(meme));
    }

    public void reindex(Meme meme) {
        upsertVectors(List.of(meme));
    }

    public void upsertVectors(List<Meme> memes) {
        ensureIndexHost();
        if (!isConfigured()) {
            log.warn("Pinecone not fully configured. Missing: {}. Skipping upsert.", missingConfig());
            return;
        }
        try {
            String body = buildUpsertBody(memes);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexHost + "/vectors/upsert"))
                .header("Content-Type", "application/json")
                .header("Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("Pinecone upsert success for {} memes", memes.size());
            } else {
                log.error("Pinecone upsert failed: {} - {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.error("Failed to upsert to Pinecone", e);
        }
    }

    public List<Long> query(String text, int topK) {
        // Delegate to options API with defaults
        SearchOptions options = SearchOptions.defaults().withTopK(topK);
        List<SearchHit> hits = queryWithOptions(text, options);
        return hits.stream().map(SearchHit::id).toList();
    }

    /**
     * Options-based query supporting hybrid fusion, conditional reranking, and caching.
     */
    public List<SearchHit> queryWithOptions(String query, SearchOptions options) {
        if (query == null || query.isBlank()) return List.of();

        // [추가] 쿼리 확장 로직
        String vectorQuery = query;
        String keywordQuery = query;
        if (queryRewriter.isPresent()) {
            vectorQuery = queryRewriter.get().rewrite(null, query); // userContext는 null로 전달
            keywordQuery = queryRewriter.get().expandForKeywords(query);
        }

        String cacheKey = options.cacheEnabled() ? (vectorQuery.strip().toLowerCase(java.util.Locale.ROOT) + "|" + options.cacheSignature()) : null;
        if (options.cacheEnabled()) {
            List<SearchHit> cached = cache.get(cacheKey);
            if (cached != null) return cached;
        }

        // 1) Dense search (vector) - 벡터 검색은 문장형 쿼리 사용
        List<SearchHit> denseHits = denseSearch(vectorQuery, options);

        // 2) Optional hybrid (sparse keyword)
        List<SearchHit> fused = denseHits;
        if (options.enableHybrid() && keywordSearchService.isPresent()) {
            // [수정] 확장된 키워드 쿼리를 토큰화하여 List<String>으로 전달
            List<String> keywordTokens = tokenize(keywordQuery);
            List<SearchHit> sparseHits = keywordSearchService.get().searchWithScores(keywordTokens, Math.max(options.topK(), options.lightRerankTopN()));
            fused = fuseScores(denseHits, sparseHits, options);
        }

        // 3) Conditional rerank skip by margin
        if (shouldSkipRerank(fused, options)) {
            List<SearchHit> top = takeTopK(fused, options.topK());
            cache.put(cacheKey, top, options);
            return top;
        }

        // 4) Light rerank (score-sort head only)
        List<SearchHit> light = lightRerank(fused, options);

        // 5) Optional heavy reranker on top-M
        List<SearchHit> finalHits = heavyRerankIfEnabled(query, light, options);

        List<SearchHit> top = takeTopK(finalHits, options.topK());
        cache.put(cacheKey, top, options);
        return top;
    }

    private List<SearchHit> denseSearch(String query, SearchOptions options) {
        // Prefer real Pinecone scores; fallback to rank-based if unavailable
        List<SearchHit> hits = this.queryDenseHits(query, Math.max(options.topK(), options.lightRerankTopN()));
        if (hits != null && !hits.isEmpty()) return hits;
        List<Long> ids = this.queryDenseIds(query, Math.max(options.topK(), options.lightRerankTopN()));
        java.util.List<SearchHit> out = new java.util.ArrayList<>(ids.size());
        double k = 60.0;
        for (int i = 0; i < ids.size(); i++) {
            double score = 1.0 / (k + (i + 1));
            out.add(new SearchHit(ids.get(i), score, "dense"));
        }
        return out;
    }

    private List<SearchHit> queryDenseHits(String text, int topK) {
        ensureIndexHost();
        if (!isConfigured()) {
            log.warn("Pinecone not fully configured ({}). Returning empty query result.", missingConfig());
            return List.of();
        }
        try {
            float[] v = embeddingService.embed(text);
            v = ensureVectorDimension(v);
            String vec = arrayToJson(v);
            String body = "{" +
                "\"vector\":" + vec + "," +
                "\"topK\":" + topK + "," +
                "\"namespace\":\"" + namespace + "\"" +
                "}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexHost + "/query"))
                .header("Content-Type", "application/json")
                .header("Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                var matches = JsonLightParser.extractMatches(resp.body());
                java.util.List<SearchHit> out = new java.util.ArrayList<>(matches.size());
                for (var m : matches) out.add(new SearchHit(m.id, m.score, "dense"));
                return out;
            } else {
                log.error("Pinecone query failed: {} - {}", resp.statusCode(), resp.body());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Failed to query Pinecone", e);
            return List.of();
        }
    }

    private List<Long> queryDenseIds(String text, int topK) {
        ensureIndexHost();
        if (!isConfigured()) {
            log.warn("Pinecone not fully configured ({}). Returning empty query result.", missingConfig());
            return List.of();
        }
        try {
            float[] v = embeddingService.embed(text);
            v = ensureVectorDimension(v);
            String vec = arrayToJson(v);
            String body = "{" +
                "\"vector\":" + vec + "," +
                "\"topK\":" + topK + "," +
                "\"namespace\":\"" + namespace + "\"" +
                "}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexHost + "/query"))
                .header("Content-Type", "application/json")
                .header("Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return JsonLightParser.extractIds(resp.body());
            } else {
                log.error("Pinecone query failed: {} - {}", resp.statusCode(), resp.body());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Failed to query Pinecone", e);
            return List.of();
        }
    }

    private List<SearchHit> fuseScores(List<SearchHit> dense, List<SearchHit> sparse, SearchOptions options) {
        java.util.Map<Long, Double> d = rankToScoreMap(dense);
        java.util.Map<Long, Double> s = rankToScoreMap(sparse);
        java.util.Set<Long> ids = new java.util.HashSet<>();
        ids.addAll(d.keySet());
        ids.addAll(s.keySet());
        double dw = options.denseWeight();
        double sw = options.sparseWeight();
        java.util.List<SearchHit> fused = new java.util.ArrayList<>(ids.size());
        for (Long id : ids) {
            double ds = d.getOrDefault(id, 0.0);
            double ss = s.getOrDefault(id, 0.0);
            double score = ds * dw + ss * sw;
            fused.add(new SearchHit(id, score, "fused"));
        }
        fused.sort(java.util.Comparator.comparingDouble(SearchHit::score).reversed());
        return takeTopK(fused, Math.max(options.topK(), options.lightRerankTopN()));
    }

    private java.util.Map<Long, Double> rankToScoreMap(java.util.List<SearchHit> hits) {
        double k = 60.0;
        java.util.Map<Long, Double> m = new java.util.HashMap<>();
        for (int i = 0; i < hits.size(); i++) {
            long id = hits.get(i).id();
            double rrf = 1.0 / (k + (i + 1));
            m.merge(id, rrf, Double::sum);
        }
        return m;
    }

    private boolean shouldSkipRerank(java.util.List<SearchHit> hits, SearchOptions options) {
        if (hits.size() < 2) return true;
        SearchHit a = hits.get(0);
        SearchHit b = hits.get(1);
        return (a.score() - b.score()) >= options.skipIfMarginGte();
    }

    private java.util.List<SearchHit> lightRerank(java.util.List<SearchHit> hits, SearchOptions options) {
        int n = Math.min(options.lightRerankTopN(), hits.size());
        java.util.List<SearchHit> head = new java.util.ArrayList<>(hits.subList(0, n));
        head.sort(java.util.Comparator.comparingDouble(SearchHit::score).reversed());
        java.util.List<SearchHit> tail = hits.subList(n, hits.size());
        java.util.List<SearchHit> out = new java.util.ArrayList<>(hits.size());
        out.addAll(head);
        out.addAll(tail);
        return out;
    }

    private java.util.List<SearchHit> heavyRerankIfEnabled(String query, java.util.List<SearchHit> hits, SearchOptions options) {
        if (heavyReranker == null || heavyReranker.isEmpty() || options.heavyRerankTopM() <= 0) return hits;
        int m = Math.min(options.heavyRerankTopM(), hits.size());
        java.util.List<SearchHit> head = new java.util.ArrayList<>(hits.subList(0, m));
        java.util.List<SearchHit> reranked = heavyReranker.get().rerank(query, head);
        java.util.List<SearchHit> out = new java.util.ArrayList<>(hits.size());
        out.addAll(reranked);
        out.addAll(hits.subList(m, hits.size()));
        return out;
    }

    private <T> java.util.List<T> takeTopK(java.util.List<T> list, int k) {
        if (list.size() <= k) return list;
        return new java.util.ArrayList<>(list.subList(0, k));
    }

    private void ensureIndexHost() {
        if (indexHost != null && !indexHost.isBlank()) return;
        if (apiKey == null || apiKey.isBlank()) return; // cannot resolve without API key
        if (indexName == null || indexName.isBlank()) return;

        // 1) Try pod-based controller resolution when environment is provided
        if (environment != null && !environment.isBlank()) {
            try {
                String controllerUrl = "https://controller." + environment + ".pinecone.io/databases/" + indexName;
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(controllerUrl))
                    .header("Api-Key", apiKey)
                    .GET()
                    .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    String json = resp.body();
                    String host = HostParser.extractHost(json);
                    Integer dim = HostParser.extractDimension(json);
                    if (host != null && !host.isBlank()) {
                        indexHost = host.startsWith("http") ? host : ("https://" + host);
                        if (dim != null && dim > 0) {
                            resolvedIndexDimension = dim;
                            log.info("Resolved Pinecone host via controller (pod-based): {}, dimension: {}", indexHost, resolvedIndexDimension);
                        } else {
                            log.info("Resolved Pinecone host via controller (pod-based): {}", indexHost);
                        }
                        return;
                    } else {
                        log.warn("Could not resolve Pinecone host from controller response. Will try serverless API. env={}, index={}", environment, indexName);
                    }
                } else {
                    log.warn("Controller call to resolve Pinecone host failed (pod-based): {} - {}. Will try serverless API next.", resp.statusCode(), resp.body());
                }
            } catch (Exception e) {
                log.warn("Failed to resolve Pinecone host via controller (env={}, index={}). Will try serverless API next.", environment, indexName, e);
            }
        }

        // 2) Try serverless Describe Index API
        try {
            String serverlessUrl = "https://api.pinecone.io/indexes/" + indexName;
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(serverlessUrl))
                .header("Api-Key", apiKey)
                .GET()
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String json = resp.body();
                String host = HostParser.extractHost(json);
                Integer dim = HostParser.extractDimension(json);
                if (host != null && !host.isBlank()) {
                    indexHost = host.startsWith("http") ? host : ("https://" + host);
                    if (dim != null && dim > 0) {
                        resolvedIndexDimension = dim;
                        log.info("Resolved Pinecone host via serverless API: {}, dimension: {}", indexHost, resolvedIndexDimension);
                    } else {
                        log.info("Resolved Pinecone host via serverless API: {}", indexHost);
                    }
                } else {
                    log.warn("Serverless API response did not contain host. Please set PINECONE_INDEX_HOST manually. index={}", indexName);
                }
            } else if (resp.statusCode() == 404) {
                log.warn("Pinecone index '{}' not found (serverless describe returned 404). Create the index in Pinecone console or set PINECONE_INDEX_HOST manually.", indexName);
            } else {
                log.warn("Serverless API call to resolve Pinecone host failed: {} - {}. Please set PINECONE_INDEX_HOST.", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("Failed to resolve Pinecone host via serverless API (index={}). Please set PINECONE_INDEX_HOST.", indexName, e);
        }
    }

    private String buildUpsertBody(List<Meme> memes) {
        // Build vectors array
        String vectors = memes.stream().map(m -> {
            float[] v = embeddingService.embed(textFor(m));
            v = ensureVectorDimension(v);
            String vec = arrayToJson(v);
            String metadata = toJson(Map.of(
                "title", nullSafe(m.getTitle()),
                "origin", nullSafe(m.getOrigin()),
                "usageContext", nullSafe(m.getUsageContext()),
                "hashtags", nullSafe(m.getHashtags()),
                "imgUrl", nullSafe(m.getImgUrl())
            ));
            return "{" +
                "\"id\":\"" + m.getId() + "\"," +
                "\"values\":" + vec + "," +
                "\"metadata\":" + metadata +
                "}";
        }).collect(Collectors.joining(","));

        return "{" +
            "\"namespace\":\"" + namespace + "\"," +
            "\"vectors\":[" + vectors + "]" +
            "}";
    }

    private String textFor(Meme m) {
        // Weighted concatenation; usageContext and hashtags prioritized for Korean semantics
        String title = spring.memewikibe.common.util.TextNormalizer.normalize(nullSafe(m.getTitle()));
        String origin = spring.memewikibe.common.util.TextNormalizer.normalize(nullSafe(m.getOrigin()));
        String usage = spring.memewikibe.common.util.TextNormalizer.normalize(nullSafe(m.getUsageContext()));
        String tags = spring.memewikibe.common.util.TextNormalizer.normalize(nullSafe(m.getHashtags()));
        StringBuilder sb = new StringBuilder();
        // weights: usage x3, tags x3, title x1, origin x1
        repeatAppend(sb, usage, 3);
        repeatAppend(sb, tags, 3);
        repeatAppend(sb, title, 1);
        repeatAppend(sb, origin, 1);
        return sb.toString();
    }

    private static void repeatAppend(StringBuilder sb, String text, int times) {
        if (text == null || text.isBlank() || times <= 0) return;
        for (int i = 0; i < times; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(text);
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && indexHost != null && !indexHost.isBlank();
    }

    private String missingConfig() {
        StringBuilder sb = new StringBuilder();
        if (apiKey == null || apiKey.isBlank()) sb.append("PINECONE_API_KEY ");
        if (indexHost == null || indexHost.isBlank()) sb.append("PINECONE_INDEX_HOST (or resolvable via controller using PINECONE_ENVIRONMENT + PINECONE_INDEX_NAME, or via serverless Describe Index API) ");
        return sb.toString().trim();
    }

    private int targetIndexDimension(int embeddingLength) {
        if (configuredIndexDimension > 0) return configuredIndexDimension;
        if (resolvedIndexDimension > 0) return resolvedIndexDimension;
        return embeddingLength;
    }

    private float[] ensureVectorDimension(float[] v) {
        int target = targetIndexDimension(v.length);
        if (v.length == target) return v;
        if (target <= 0) return v;
        float[] out = new float[target];
        if (v.length > target) {
            // truncate
            System.arraycopy(v, 0, out, 0, target);
        } else {
            // pad with zeros
            System.arraycopy(v, 0, out, 0, v.length);
            // remaining values are zeros by default
        }
        log.debug("Adjusted embedding vector dimension from {} to {} (configuredIndexDimension={}, resolvedIndexDimension={})", v.length, target, configuredIndexDimension, resolvedIndexDimension);
        return out;
    }

    private static String arrayToJson(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Float.toString(arr[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private static String toJson(Map<String, String> map) {
        return "{" + map.entrySet().stream()
            .map(e -> "\"" + escape(e.getKey()) + "\":\"" + escape(e.getValue()) + "\"")
            .collect(Collectors.joining(",")) + "}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Minimal JSON helpers without adding a JSON library.
     */
    static class HostParser {
        static String extractHost(String json) {
            // naive: look for "host":"..."
            int pos = json.indexOf("\"host\"");
            if (pos < 0) return null;
            int colon = json.indexOf(':', pos);
            int q1 = json.indexOf('"', colon + 1);
            int q2 = json.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) return null;
            return json.substring(q1 + 1, q2);
        }
        static Integer extractDimension(String json) {
            int pos = json.indexOf("\"dimension\"");
            if (pos < 0) return null;
            int colon = json.indexOf(':', pos);
            if (colon < 0) return null;
            int i = colon + 1;
            // skip spaces
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            int start = i;
            while (i < json.length() && Character.isDigit(json.charAt(i))) i++;
            if (start == i) return null;
            try {
                return Integer.parseInt(json.substring(start, i));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    static class JsonLightParser {
        static List<Long> extractIds(String json) {
            // very naive: find occurrences of "id":"..." and parse long
            new Object();
            java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
            int idx = 0;
            while (true) {
                int pos = json.indexOf("\"id\"", idx);
                if (pos < 0) break;
                int colon = json.indexOf(':', pos);
                int q1 = json.indexOf('"', colon + 1);
                int q2 = json.indexOf('"', q1 + 1);
                if (q1 < 0 || q2 < 0) break;
                String idStr = json.substring(q1 + 1, q2);
                try {
                    ids.add(Long.parseLong(idStr));
                } catch (NumberFormatException ignored) {}
                idx = q2 + 1;
            }
            return ids;
        }
        static java.util.List<Match> extractMatches(String json) {
            // Extract pairs (id, score) from Pinecone response: "matches":[{"id":"...","score":0.xx},...]
            java.util.List<Match> matches = new java.util.ArrayList<>();
            int arrPos = json.indexOf("\"matches\"");
            if (arrPos < 0) return matches;
            int lb = json.indexOf('[', arrPos);
            int rb = json.indexOf(']', lb);
            if (lb < 0 || rb < 0) return matches;
            String arr = json.substring(lb + 1, rb);
            int idx = 0;
            while (true) {
                int idKey = arr.indexOf("\"id\"", idx);
                if (idKey < 0) break;
                int colon = arr.indexOf(':', idKey);
                int q1 = arr.indexOf('"', colon + 1);
                int q2 = arr.indexOf('"', q1 + 1);
                if (q1 < 0 || q2 < 0) break;
                String idStr = arr.substring(q1 + 1, q2);
                long id;
                try { id = Long.parseLong(idStr); } catch (NumberFormatException e) { id = -1L; }
                int scoreKey = arr.indexOf("\"score\"", q2);
                if (scoreKey < 0) break;
                int scolon = arr.indexOf(':', scoreKey);
                int end = scolon + 1;
                // parse until comma or end of object
                while (end < arr.length() && "-+.0123456789eE".indexOf(arr.charAt(end)) >= 0) end++;
                double score = 0.0;
                try { score = Double.parseDouble(arr.substring(scolon + 1, end).trim()); } catch (Exception ignored) {}
                if (id >= 0) matches.add(new Match(id, score));
                idx = end;
            }
            return matches;
        }
        static final class Match { final long id; final double score; Match(long i,double s){id=i;score=s;} }
    }

    // --- Options / DTOs / Interfaces for hybrid + rerank + cache ---
    public record SearchHit(Long id, double score, String source) {}

    public record SearchOptions(
        int topK,
        int lightRerankTopN,
        int heavyRerankTopM,
        double skipIfMarginGte,
        boolean enableHybrid,
        double sparseWeight,
        double denseWeight,
        Integer efSearch,
        boolean cacheEnabled
    ) {
        public static SearchOptions defaults() {
            return new SearchOptions(
                100, // topK
                40,  // lightRerankTopN
                15,  // heavyRerankTopM
                0.12, // skipIfMarginGte
                false, // enableHybrid
                0.3, // sparseWeight
                0.7, // denseWeight
                null, // efSearch
                true  // cacheEnabled
            );
        }
        public SearchOptions withTopK(int k) {
            return new SearchOptions(k, lightRerankTopN, heavyRerankTopM, skipIfMarginGte, enableHybrid, sparseWeight, denseWeight, efSearch, cacheEnabled);
        }
        public String cacheSignature() {
            return String.join(":",
                String.valueOf(topK),
                String.valueOf(lightRerankTopN),
                String.valueOf(heavyRerankTopM),
                String.valueOf(skipIfMarginGte),
                String.valueOf(enableHybrid),
                String.valueOf(sparseWeight),
                String.valueOf(denseWeight),
                String.valueOf(efSearch),
                String.valueOf(cacheEnabled)
            );
        }
    }

    public interface Reranker {
        java.util.List<SearchHit> rerank(String query, java.util.List<SearchHit> candidates);
    }

    public interface KeywordSearchService {
        // 신규: 지능화된 List<String> 버전
        java.util.List<SearchHit> searchWithScores(List<String> keywords, int topK);

        // 기존: 하위 호환성을 위한 String 버전
        java.util.List<SearchHit> searchWithScores(String query, int topK);
    }

    public interface MemeDocumentProvider {
        String textOf(Long memeId);
    }

    static final class SearchCache {
        private final java.time.Duration ttl;
        private final java.util.Map<String, Entry> map = new java.util.concurrent.ConcurrentHashMap<>();
        SearchCache(java.time.Duration ttl) { this.ttl = ttl; }
        java.util.List<SearchHit> get(String key) {
            if (key == null) return null;
            Entry e = map.get(key);
            if (e == null || java.time.Instant.now().isAfter(e.expireAt)) {
                map.remove(key);
                return null;
            }
            return e.value;
        }
        void put(String key, java.util.List<SearchHit> value, SearchOptions options) {
            if (key == null || !options.cacheEnabled()) return;
            map.put(key, new Entry(value, java.time.Instant.now().plus(ttl)));
        }
        record Entry(java.util.List<SearchHit> value, java.time.Instant expireAt) {}
    }

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,]+");
    private List<String> tokenize(String s) {
        if (s == null) return List.of();
        return Arrays.stream(TOKEN_SPLIT.split(s.toLowerCase()))
            .filter(tok -> !tok.isBlank()).toList();
    }
}
