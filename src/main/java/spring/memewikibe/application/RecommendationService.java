package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.recommendation.response.MemeRecommendationResponse;
import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MemeVectorIndexService vectorIndexService;
    private final MemeRepository memeRepository;
    private final spring.memewikibe.infrastructure.ai.NaverRagService naverRagService;
    private final SafeFullTextSearchExecutor safeFts;

    // Tokenize by whitespace and punctuation; keep it simple and robust
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s\\p{Punct}]+");

    @org.springframework.beans.factory.annotation.Value("${recommend.alpha:0.6}")
    private double alpha; // vector vs keyword blend

    @org.springframework.beans.factory.annotation.Value("${recommend.mmr.lambda:0.7}")
    private double mmrLambda;

    @org.springframework.beans.factory.annotation.Value("${recommend.vector.topK:200}")
    private int vecTopK;

    @org.springframework.beans.factory.annotation.Value("${recommend.keyword.topK:200}")
    private int kwTopK;

    @org.springframework.beans.factory.annotation.Value("${recommend.reranker.topN:50}")
    private int rerankerTopN;

    @org.springframework.beans.factory.annotation.Value("${recommend.reranker.enabled:false}")
    private boolean rerankerEnabled;

    @org.springframework.beans.factory.annotation.Value("${recommend.hybrid.adaptive:true}")
    private boolean adaptiveHybrid;

    // LLM-based query rewrite toggle
    @org.springframework.beans.factory.annotation.Value("${recommend.rewrite.enabled:true}")
    private boolean rewriteEnabled;

    // Cross-encoder reranker (after MMR) toggles
    @org.springframework.beans.factory.annotation.Value("${recommend.cross.enabled:true}")
    private boolean crossEnabled;
    @org.springframework.beans.factory.annotation.Value("${recommend.cross.topM:20}")
    private int crossTopM;

    // Evaluation logging toggle
    @org.springframework.beans.factory.annotation.Value("${recommend.eval.log.enabled:true}")
    private boolean evalLogEnabled;

    // Optional components
    private final java.util.Optional<spring.memewikibe.infrastructure.ai.QueryRewriter> queryRewriter;
    private final java.util.Optional<spring.memewikibe.infrastructure.ai.CrossEncoderReranker> crossEncoder;

    @Transactional(readOnly = true)
    public List<MemeRecommendationResponse> searchWithReasons(String query, Long userId, int limit) {
        long t0 = System.nanoTime();
        String userContext = (userId == null) ? "" : ("user:" + userId);

        // Stage 0: LLM-based query rewrite (optional)
        String qIn = query == null ? "" : query;
        String rewritten = (rewriteEnabled && queryRewriter != null && queryRewriter.isPresent())
            ? queryRewriter.get().rewrite(userContext, qIn)
            : qIn;
        String normQuery = spring.memewikibe.common.util.TextNormalizer.normalize(rewritten);
        List<String> qTokens = tokenize(normQuery);
        if (qTokens.isEmpty()) return List.of();
        long tRewrite = System.nanoTime();

        // 1) Vector candidates
        int useVecTopK = Math.max(50, Math.min(400, vecTopK));
        List<Long> vecIds = vectorIndexService.query(normQuery, useVecTopK);

        // 2) Keyword candidates via MySQL Full-Text Search (isolated from TX); fallback to QueryDSL for H2/local
        int useKwTopK = Math.max(50, Math.min(400, kwTopK));
        List<Meme> kwCandidates = safeFts.tryFullText(normQuery, useKwTopK);
        if (kwCandidates.isEmpty()) {
            try {
                kwCandidates = memeRepository.findKeywordCandidatesAcrossFields(normQuery, org.springframework.data.domain.Limit.of(useKwTopK));
            } catch (Throwable t2) {
                kwCandidates = memeRepository.findByTitleOrHashtagsContainingOrderByIdDesc(normQuery, org.springframework.data.domain.Limit.of(useKwTopK));
            }
        }

        Map<Long, Integer> vecRank = new HashMap<>();
        for (int i = 0; i < vecIds.size(); i++) vecRank.put(vecIds.get(i), i);
        Map<Long, Meme> kwById = kwCandidates.stream().collect(Collectors.toMap(Meme::getId, Function.identity(), (a,b)->a));

        // Batch-fetch memes that appear only in vector results (not in keyword candidates)
        if (!vecIds.isEmpty()) {
            Set<Long> missing = new LinkedHashSet<>(vecIds);
            missing.removeAll(kwById.keySet());
            if (!missing.isEmpty()) {
                List<Meme> fetched = memeRepository.findAllById(missing);
                for (Meme m : fetched) {
                    // Only include NORMAL memes
                    if (m != null && m.getFlag() == spring.memewikibe.domain.meme.Meme.Flag.NORMAL) {
                        kwById.put(m.getId(), m);
                    }
                }
            }
        }

        // 3) Score + blend
        Map<Long, Double> vecScore = new HashMap<>();
        double maxVecRank = Math.max(1, vecIds.size());
        for (Long id : vecIds) {
            int r = vecRank.get(id);
            // convert rank to score in 0..1 (higher is better)
            double s = 1.0 - (r / maxVecRank);
            vecScore.put(id, s);
        }

        Map<Long, Double> kwScore = new HashMap<>();
        for (Meme m : kwCandidates) {
            double s = keywordMatchScore(normQuery, qTokens, m);
            kwScore.put(m.getId(), s);
        }
        // Add keyword scores for vector-only fetched memes, too (keep hybrid scoring consistent)
        for (Map.Entry<Long, Meme> e : kwById.entrySet()) {
            if (!kwScore.containsKey(e.getKey())) {
                kwScore.put(e.getKey(), keywordMatchScore(normQuery, qTokens, e.getValue()));
            }
        }
        // Normalize kw scores
        double maxKw = kwScore.values().stream().mapToDouble(d -> d).max().orElse(1.0);
        if (maxKw > 0) kwScore.replaceAll((k,v) -> v / maxKw);
        double alphaEff = alpha;
        if (adaptiveHybrid) {
            long kwHits = kwScore.values().stream().filter(v -> v > 0.0).count();
            if (kwHits > 0) {
                // If we have any keyword evidence, lean more on keywords to improve Korean literal/usage matches
                alphaEff = Math.min(alpha, 0.4); // favor keywords more
            } else {
                // No keyword evidence at all → rely on vector a bit more
                alphaEff = Math.max(alpha, 0.8);
            }
        }

        // union of ids
        Set<Long> allIds = new LinkedHashSet<>();
        allIds.addAll(vecIds);
        allIds.addAll(kwById.keySet());

        List<Scored> blended = new ArrayList<>();
        for (Long id : allIds) {
            Meme m = kwById.get(id);
            if (m == null) continue; // ensure entity present (now includes vector-only fetched)
            double vs = vecScore.getOrDefault(id, 0.0);
            double ks = kwScore.getOrDefault(id, 0.0);
            double combined = alphaEff * vs + (1 - alphaEff) * ks;
            ScoreResult sr = scoreAndExplain(normQuery, qTokens, m, vecRank.getOrDefault(id, Integer.MAX_VALUE));
            // Attach combined score but keep reason from content-based explainer
            blended.add(new Scored(m, combined, sr.reason));
        }

        // 4) MMR diversification over blended list
        List<Scored> mmred = mmr(blended, qTokens, Math.min(rerankerTopN, blended.size()));

        // 5) Cross-Encoder reranking (optional)
        List<Scored> afterCE = mmred;
        if (crossEnabled && crossEncoder != null && crossEncoder.isPresent() && !mmred.isEmpty()) {
            int m = Math.min(Math.max(5, crossTopM), mmred.size());
            List<Scored> head = new ArrayList<>(mmred.subList(0, m));
            List<spring.memewikibe.infrastructure.ai.CrossEncoderReranker.Candidate> ceCands = head.stream()
                .map(s -> new spring.memewikibe.infrastructure.ai.CrossEncoderReranker.Candidate(
                    s.meme.getId(), s.meme.getTitle(), s.meme.getUsageContext(), s.meme.getHashtags(), s.score
                ))
                .toList();
            List<Long> ceOrder = crossEncoder.get().rerank(normQuery, ceCands);
            Map<Long, Integer> rank = new HashMap<>();
            for (int i = 0; i < ceOrder.size(); i++) rank.put(ceOrder.get(i), i);
            head.sort((a,b) -> Integer.compare(rank.getOrDefault(a.meme.getId(), Integer.MAX_VALUE), rank.getOrDefault(b.meme.getId(), Integer.MAX_VALUE)));
            afterCE = new ArrayList<>(head);
            afterCE.addAll(mmred.subList(m, mmred.size()));
        }

        // Keep legacy light heuristic reranker as an extra knob if desired
        List<Scored> finalList = rerankerEnabled ? heuristicRerank(normQuery, afterCE) : afterCE;

        // 6) Naver RAG rerank (default path). We pass top-N ids to RAG and reorder accordingly.
        int outLimit = Math.max(1, Math.min(50, limit));
        int ragTopN = Math.min(Math.max(outLimit, 3), Math.min(50, finalList.size()));
        java.util.List<spring.memewikibe.infrastructure.ai.NaverRagService.Candidate> ragCandidates = finalList.stream()
                .limit(ragTopN)
                .map(s -> new spring.memewikibe.infrastructure.ai.NaverRagService.Candidate(
                        s.meme.getId(), s.meme.getTitle(), s.meme.getUsageContext(), s.meme.getHashtags()
                ))
                .toList();
        spring.memewikibe.infrastructure.ai.NaverRagService.RagResult rag = naverRagService.recommendWithContextDetailed(userContext, normQuery, ragCandidates);
        java.util.List<Long> ragOrdered = rag.ids();
        java.util.Map<Long, Integer> ragOrder = new java.util.HashMap<>();
        for (int i = 0; i < ragOrdered.size(); i++) ragOrder.put(ragOrdered.get(i), i);
        java.util.Comparator<Scored> ragComparator = (a, b) -> {
            Integer ia = ragOrder.get(a.meme.getId());
            Integer ib = ragOrder.get(b.meme.getId());
            if (ia == null && ib == null) return Double.compare(b.score, a.score);
            if (ia == null) return 1; // items not in RAG head go after those in head
            if (ib == null) return -1;
            return Integer.compare(ia, ib);
        };
        java.util.List<Scored> ragReranked = new java.util.ArrayList<>(finalList);
        // Only reorder the head by RAG, keep tail after head in original order
        java.util.List<Scored> head = new java.util.ArrayList<>(ragReranked.subList(0, ragTopN));
        head.sort(ragComparator);
        java.util.List<Scored> tail = new java.util.ArrayList<>(ragReranked.subList(ragTopN, ragReranked.size()));
        ragReranked.clear();
        ragReranked.addAll(head);
        ragReranked.addAll(tail);

        // fetch images/titles already available; limit and map
        java.util.List<Scored> limited = ragReranked.stream().limit(outLimit).toList();
        java.util.List<MemeRecommendationResponse> responses = new java.util.ArrayList<>(limited.size());
        for (int i = 0; i < limited.size(); i++) {
            Scored s = limited.get(i);
            String reasonOut = (i == 0) ? rag.reason() : s.reason;
            responses.add(new MemeRecommendationResponse(s.meme.getId(), s.meme.getTitle(), s.meme.getImgUrl(), reasonOut));
        }

        long tEnd = System.nanoTime();
        if (evalLogEnabled) {
            log.info("[SearchExplainEval] rewrite={}ms, vecK={}, kwK={}, fused={}, mmrHead={}, ceEnabled={}, ceM={}, ragHead={}, total={}ms",
                msBetween(t0, tRewrite), vecIds.size(), kwCandidates.size(), blended.size(), Math.min(rerankerTopN, blended.size()),
                crossEnabled, Math.min(Math.max(5, crossTopM), Math.max(0, mmred.size())), ragTopN, msBetween(t0, tEnd));
        }
        return responses;
    }

    private static long msBetween(long tStart, long tEnd) { return (tEnd - tStart) / 1_000_000L; }

    // Keyword/BM25-ish score with field weights favoring usage_context and hashtags
    private double keywordMatchScore(String qLower, List<String> qTokens, Meme m) {
        String usage = safeLower(m.getUsageContext());
        String title = safeLower(m.getTitle());
        String origin = safeLower(m.getOrigin());
        List<String> tags = HashtagParser.parseHashtags(m.getHashtags()).stream()
            .map(s -> s == null ? "" : s.replace("#", "").toLowerCase())
            .filter(s -> !s.isBlank())
            .toList();
        int qSize = Math.max(1, qTokens.size());
        int usageMatches = countContainsTokens(usage, qTokens);
        int titleMatches = countContainsTokens(title, qTokens);
        int originMatches = countContainsTokens(origin, qTokens);
        int tagMatches = countTagMatches(tags, qTokens);
        boolean usageContainsPhrase = usage.contains(qLower);
        boolean titleContainsPhrase = title.contains(qLower);
        boolean tagEqualsQuery = tags.stream().anyMatch(t -> t.equals(qLower.replace("#", "")));
        boolean tagContainsQuery = tags.stream().anyMatch(t -> qLower.contains(t) || t.contains(qLower));
        double score = 0.0;
        score += 0.55 * (usageMatches / (double) qSize);
        score += 0.30 * (tagMatches / (double) qSize);
        score += 0.10 * (titleMatches / (double) qSize);
        score += 0.05 * (originMatches / (double) qSize);
        if (usageContainsPhrase) score += 0.10;
        if (titleContainsPhrase) score += 0.03;
        if (tagEqualsQuery) score += 0.10; else if (tagContainsQuery) score += 0.05;
        return Math.max(0.0, score);
    }

    // MMR diversification using token Jaccard similarity
    private List<Scored> mmr(List<Scored> items, List<String> qTokens, int k) {
        if (items.isEmpty() || k <= 0) return List.of();
        // Precompute token sets for each document
        Map<Long, Set<String>> docTokens = new HashMap<>();
        for (Scored s : items) {
            Set<String> set = new LinkedHashSet<>(tokenize(safeLower(s.meme.getTitle() + " " + s.meme.getUsageContext() + " " + s.meme.getHashtags())));
            docTokens.put(s.meme.getId(), set);
        }
        List<Scored> selected = new ArrayList<>();
        List<Scored> candidates = new ArrayList<>(items);
        while (!candidates.isEmpty() && selected.size() < k) {
            Scored best = null;
            double bestVal = -1e9;
            for (Scored c : candidates) {
                double relevance = c.score; // already blended
                double diversityPenalty = 0.0;
                for (Scored s : selected) {
                    diversityPenalty = Math.max(diversityPenalty, jaccard(docTokens.get(c.meme.getId()), docTokens.get(s.meme.getId())));
                }
                double mmrScore = mmrLambda * relevance - (1 - mmrLambda) * diversityPenalty;
                if (mmrScore > bestVal) {
                    bestVal = mmrScore;
                    best = c;
                }
            }
            if (best == null) break;
            selected.add(best);
            candidates.remove(best);
        }
        return selected;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;
        int inter = 0;
        for (String x : a) if (b.contains(x)) inter++;
        int uni = a.size() + b.size() - inter;
        return uni == 0 ? 0.0 : (inter / (double) uni);
    }

    // Heuristic reranker placeholder (acts as cross-encoder slot): boost exact phrase and strong usage/tag signals
    private List<Scored> heuristicRerank(String normQuery, List<Scored> list) {
        return list.stream()
            .map(s -> {
                double bonus = 0.0;
                String usage = safeLower(s.meme.getUsageContext());
                String title = safeLower(s.meme.getTitle());
                String tags = safeLower(String.join(" ", HashtagParser.parseHashtags(s.meme.getHashtags())));
                if (usage.contains(normQuery)) bonus += 0.08;
                if (title.contains(normQuery)) bonus += 0.03;
                if (tags.contains(normQuery)) bonus += 0.04;
                return new Scored(s.meme, s.score + bonus, s.reason);
            })
            .sorted((a,b) -> Double.compare(b.score, a.score))
            .toList();
    }

    private static class Scored {
        final Meme meme;
        final double score;
        final String reason;
        Scored(Meme meme, double score, String reason) { this.meme = meme; this.score = score; this.reason = reason; }
    }

    private record ScoreResult(double score, String reason) {}

    private ScoreResult scoreAndExplain(String qLower, List<String> qTokens, Meme m, int rankIndex) {
        // Weights: usage_context 0.5, hashtags 0.35, title 0.1, origin 0.05 + small rank bonus
        String usage = safeLower(m.getUsageContext());
        String title = safeLower(m.getTitle());
        String origin = safeLower(m.getOrigin());
        List<String> tags = HashtagParser.parseHashtags(m.getHashtags()).stream()
            .map(s -> s == null ? "" : s.replace("#", "").toLowerCase())
            .filter(s -> !s.isBlank())
            .toList();

        int qSize = Math.max(1, qTokens.size());

        int usageMatches = countContainsTokens(usage, qTokens);
        int titleMatches = countContainsTokens(title, qTokens);
        int originMatches = countContainsTokens(origin, qTokens);
        int tagMatches = countTagMatches(tags, qTokens);

        boolean usageContainsPhrase = usage.contains(qLower);
        boolean titleContainsPhrase = title.contains(qLower);
        boolean tagEqualsQuery = tags.stream().anyMatch(t -> t.equals(qLower.replace("#", "")));
        boolean tagContainsQuery = tags.stream().anyMatch(t -> qLower.contains(t) || t.contains(qLower));

        double score = 0.0;
        score += 0.5 * (usageMatches / (double) qSize);
        score += 0.35 * (tagMatches / (double) qSize);
        score += 0.10 * (titleMatches / (double) qSize);
        score += 0.05 * (originMatches / (double) qSize);
        if (usageContainsPhrase) score += 0.15;
        if (titleContainsPhrase) score += 0.05;
        if (tagEqualsQuery) score += 0.20; else if (tagContainsQuery) score += 0.10;
        // small vector-rank prior
        score += 0.10 * (1.0 / (rankIndex + 1));

        // reason
        List<String> reasons = new ArrayList<>();
        if (usageContainsPhrase || usageMatches > 0) {
            if (usageContainsPhrase) reasons.add("사용 맥락에 검색어가 직접 등장");
            if (usageMatches > 0) reasons.add("사용 맥락 키워드 " + usageMatches + "개 일치");
        }
        if (tagMatches > 0) {
            List<String> matchedTags = findMatchedTags(tags, qTokens).stream().limit(3).toList();
            if (!matchedTags.isEmpty()) {
                String tagStr = matchedTags.stream().map(t -> "#" + t).collect(Collectors.joining(", "));
                reasons.add("해시태그 " + tagStr + " 연관");
            } else {
                reasons.add("해시태그 키워드 일치");
            }
        }
        if (titleContainsPhrase || titleMatches > 0) {
            reasons.add("제목 관련 키워드 일치");
        }
        if (originMatches > 0) {
            reasons.add("출처 관련 키워드 일치");
        }
        if (reasons.isEmpty()) {
            reasons.add("벡터 의미 유사도 기반 추천");
        }
        String reason = String.join(" · ", reasons);
        return new ScoreResult(score, reason);
    }

    private static int countContainsTokens(String text, List<String> tokens) {
        if (text.isBlank() || tokens.isEmpty()) return 0;
        int c = 0;
        for (String t : tokens) {
            if (t.isBlank()) continue;
            if (text.contains(t)) c++;
        }
        return c;
    }

    private static int countTagMatches(List<String> tags, List<String> tokens) {
        if (tags.isEmpty() || tokens.isEmpty()) return 0;
        int c = 0;
        for (String t : tokens) {
            String tt = t.replace("#", "");
            for (String tag : tags) {
                if (tag.equals(tt) || tag.contains(tt) || tt.contains(tag)) {
                    c++;
                    break;
                }
            }
        }
        return c;
    }

    private static List<String> findMatchedTags(List<String> tags, List<String> tokens) {
        Set<String> res = new LinkedHashSet<>();
        for (String t : tokens) {
            String tt = t.replace("#", "");
            for (String tag : tags) {
                if (tag.equals(tt) || tag.contains(tt) || tt.contains(tag)) {
                    res.add(tag);
                }
            }
        }
        return new ArrayList<>(res);
    }

    private static List<String> tokenize(String s) {
        if (s == null) return List.of();
        return Arrays.stream(TOKEN_SPLIT.split(s.toLowerCase()))
            .filter(tok -> !tok.isBlank())
            .toList();
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}
