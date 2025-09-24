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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemeVectorIndexService {

    private final EmbeddingService embeddingService;

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
                // very light parsing to extract ids: assumes JSON contains matches:[{id: "123", score:...},...]
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
        return String.join(" \n ",
            nullSafe(m.getTitle()),
            nullSafe(m.getOrigin()),
            nullSafe(m.getUsageContext()),
            nullSafe(m.getTrendPeriod()),
            nullSafe(m.getHashtags())
        );
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
    }
}
