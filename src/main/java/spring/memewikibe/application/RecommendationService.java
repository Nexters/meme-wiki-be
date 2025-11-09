package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.recommendation.response.MemeRecommendationResponse;
import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.common.util.TextNormalizer;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;
import spring.memewikibe.infrastructure.ai.NaverRagService;
import spring.memewikibe.infrastructure.ai.QueryRewriter;
import spring.memewikibe.infrastructure.ai.CrossEncoderReranker;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Limit.*;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MemeVectorIndexService vectorIndexService;
    private final MemeRepository memeRepository;
    private final NaverRagService naverRagService;
    private final SafeFullTextSearchExecutor safeFts;

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s\\p{Punct}]+");

    private static final Set<String> STOP_WORDS = Set.of(
        "은", "는", "이", "가", "을", "를", "의", "에", "에서", "에게", "께",
        "으로", "로", "과", "와", "도", "만", "까지", "부터", "하다", "되다",
        "이다", "있다", "없다", "싶다", "지", "요", "고", "다", "음", "면",
        "것", "수", "등", "대한"
    );

    private static final int MIN_VEC_TOP_K = 50;
    private static final int MAX_VEC_TOP_K = 400;
    private static final int MIN_KW_TOP_K = 50;
    private static final int MAX_KW_TOP_K = 400;
    private static final int MIN_CROSS_ENCODER_TOP_M = 5;
    private static final int MIN_OUTPUT_LIMIT = 1;
    private static final int MAX_OUTPUT_LIMIT = 50;

    @Value("${recommend.alpha:0.6}")
    private double alpha;
    @Value("${recommend.mmr.lambda:0.7}")
    private double mmrLambda;
    @Value("${recommend.vector.topK:200}")
    private int vecTopK;
    @Value("${recommend.keyword.topK:200}")
    private int kwTopK;
    @Value("${recommend.reranker.topN:50}")
    private int rerankerTopN;
    @Value("${recommend.reranker.enabled:false}")
    private boolean rerankerEnabled;
    @Value("${recommend.hybrid.adaptive:true}")
    private boolean adaptiveHybrid;
    @Value("${recommend.rewrite.enabled:true}")
    private boolean rewriteEnabled;
    @Value("${recommend.cross.enabled:true}")
    private boolean crossEnabled;
    @Value("${recommend.cross.topM:20}")
    private int crossTopM;
    @Value("${recommend.eval.log.enabled:true}")
    private boolean evalLogEnabled;

    private final Optional<QueryRewriter> queryRewriter;
    private final Optional<CrossEncoderReranker> crossEncoder;

    @Transactional(readOnly = true)
    public List<MemeRecommendationResponse> searchWithReasons(String query, Long userId, int limit) {
        long t0 = System.nanoTime();
        String userContext = (userId == null) ? "" : ("user:" + userId);

        String qIn = query == null ? "" : query;
        if (qIn.isBlank()) return List.of();

        // [핵심 변경] Stage 0: 쿼리 이해 및 확장 단계
        String vectorQuery = qIn; // 벡터 검색은 원본 또는 재작성된 문장형 쿼리 사용
        String keywordQuery = qIn; // 키워드 검색은 확장된 키워드 쿼리 사용
        if (rewriteEnabled && queryRewriter.isPresent()) {
            vectorQuery = queryRewriter.get().rewrite(userContext, qIn);
            keywordQuery = queryRewriter.get().expandForKeywords(qIn); // 예: "회사그만둬야지" -> "퇴사 사직 회사"
        }

        String normVectorQuery = TextNormalizer.normalize(vectorQuery);
        String normKeywordQuery = TextNormalizer.normalize(keywordQuery);
        List<String> keywordTokens = tokenize(normKeywordQuery);
        if (keywordTokens.isEmpty()) return List.of();

        // 1) Vector candidates - 의미적 유사도 기반
        int useVecTopK = Math.max(MIN_VEC_TOP_K, Math.min(MAX_VEC_TOP_K, vecTopK));
        List<Long> vecIds = vectorIndexService.query(normVectorQuery, useVecTopK);

        // 2) Keyword candidates - 확장된 키워드 기반
        int useKwTopK = Math.max(MIN_KW_TOP_K, Math.min(MAX_KW_TOP_K, kwTopK));

        // Full-Text Search를 먼저 시도하고, 실패 시 새로운 OR 검색 메소드를 호출
        List<Meme> kwCandidates = safeFts.tryFullText(normKeywordQuery, useKwTopK);
        if (kwCandidates.isEmpty()) {
            try {
                // 확장된 키워드 토큰들로 OR 조건 검색을 수행
                log.info("Performing keyword search with OR conditions for tokens: {}", keywordTokens);
                kwCandidates = memeRepository.findKeywordCandidatesAcrossFields(keywordTokens, of(useKwTopK));
            } catch (Throwable t2) {
                log.error("Custom keyword search failed, falling back to simple containing search.", t2);
                kwCandidates = memeRepository.findByTitleOrHashtagsContainingOrderByIdDesc(normKeywordQuery, of(useKwTopK));
            }
        }

        Map<Long, Integer> vecRank = new HashMap<>();
        for (int i = 0; i < vecIds.size(); i++) vecRank.put(vecIds.get(i), i);
        Map<Long, Meme> kwById = kwCandidates.stream().collect(Collectors.toMap(Meme::getId, Function.identity(), (a, b) -> a));

        if (!vecIds.isEmpty()) {
            Set<Long> missing = new LinkedHashSet<>(vecIds);
            missing.removeAll(kwById.keySet());
            if (!missing.isEmpty()) {
                List<Meme> fetched = memeRepository.findAllById(missing);
                for (Meme m : fetched) {
                    if (m != null && m.getFlag() == Meme.Flag.NORMAL) {
                        kwById.put(m.getId(), m);
                    }
                }
            }
        }

        // 3) Score + blend - 점수 계산 시 확장된 키워드 사용
        Map<Long, Double> vecScore = new HashMap<>();
        double maxVecRank = Math.max(1, vecIds.size());
        vecIds.forEach(id -> vecScore.put(id, 1.0 - (vecRank.getOrDefault(id, useVecTopK) / maxVecRank)));

        Map<Long, Double> kwScore = new HashMap<>();
        kwById.values().forEach(m -> kwScore.put(m.getId(), keywordMatchScore(normKeywordQuery, keywordTokens, m)));

        double maxKw = kwScore.values().stream().mapToDouble(d -> d).max().orElse(1.0);
        if (maxKw > 0) kwScore.replaceAll((k, v) -> v / maxKw);

        double alphaEff = alpha;
        if (adaptiveHybrid) {
            long kwHits = kwScore.values().stream().filter(v -> v > 0.0).count();
            alphaEff = (kwHits > 0) ? Math.min(alpha, 0.4) : Math.max(alpha, 0.8);
        }

        Set<Long> allIds = new LinkedHashSet<>(vecIds);
        allIds.addAll(kwById.keySet());

        List<Scored> blended = new ArrayList<>();
        for (Long id : allIds) {
            Meme m = kwById.get(id);
            if (m == null) continue;
            double vs = vecScore.getOrDefault(id, 0.0);
            double ks = kwScore.getOrDefault(id, 0.0);
            double combined = alphaEff * vs + (1 - alphaEff) * ks;
            ScoreResult sr = scoreAndExplain(normKeywordQuery, keywordTokens, m);
            blended.add(new Scored(m, combined, sr.reason));
        }

        // 4) MMR diversification
        List<Scored> mmred = mmr(blended, keywordTokens, Math.min(rerankerTopN, blended.size()));

        List<Scored> afterCE = mmred;
        if (crossEnabled && crossEncoder.isPresent() && !mmred.isEmpty()) {
            log.info("Applying CrossEncoder reranking to top {} candidates.", Math.min(crossTopM, mmred.size()));
            int m = Math.min(Math.max(MIN_CROSS_ENCODER_TOP_M, crossTopM), mmred.size());
            List<Scored> head = new ArrayList<>(mmred.subList(0, m));

            // Reranker에 전달할 후보 목록 생성
            List<CrossEncoderReranker.Candidate> ceCands = head.stream()
                .map(s -> new CrossEncoderReranker.Candidate(
                    s.meme.getId(), s.meme.getTitle(), s.meme.getUsageContext(), s.meme.getHashtags(), s.score
                ))
                .toList();

            // CrossEncoder 호출
            List<Long> ceOrder = crossEncoder.get().rerank(vectorQuery, ceCands); // 쿼리는 문장형 원본 사용

            // 반환된 순서(ceOrder)에 따라 head 리스트를 재정렬
            Map<Long, Integer> rank = new HashMap<>();
            for (int i = 0; i < ceOrder.size(); i++) rank.put(ceOrder.get(i), i);
            head.sort(Comparator.comparingInt(a -> rank.getOrDefault(a.meme.getId(), Integer.MAX_VALUE)));

            // 재정렬된 head와 원래 tail을 합쳐 최종 리스트 생성
            afterCE = new ArrayList<>(head);
            afterCE.addAll(mmred.subList(m, mmred.size()));
        }

        List<Scored> finalList = rerankerEnabled ? heuristicRerank(normKeywordQuery, afterCE) : afterCE;

        // 6) Final RAG-based Reason Generation for top result
        int outLimit = Math.max(MIN_OUTPUT_LIMIT, Math.min(MAX_OUTPUT_LIMIT, limit));
        if (finalList.isEmpty()) {
            return List.of();
        }

        Scored topCandidate = finalList.get(0);
        List<NaverRagService.Candidate> singleCandidateList = List.of(
            new NaverRagService.Candidate(topCandidate.meme.getId(), topCandidate.meme.getTitle(), topCandidate.meme.getUsageContext(), topCandidate.meme.getHashtags())
        );

        String topReasonFromRag;
        try {
            // RAG에는 의미 파악을 위해 문장형 쿼리(vectorQuery)를 전달
            NaverRagService.RagResult rag = naverRagService.recommendWithContextDetailed(userContext, vectorQuery, singleCandidateList);
            topReasonFromRag = rag.reason();
        } catch (Exception e) {
            log.warn("NaverRAGService call for reason generation failed. Falling back.", e);
            topReasonFromRag = topCandidate.reason;
        }

        List<Scored> limited = finalList.stream().limit(outLimit).toList();
        List<MemeRecommendationResponse> responses = new ArrayList<>(limited.size());
        for (int i = 0; i < limited.size(); i++) {
            Scored s = limited.get(i);
            String reasonOut = (i == 0 && topReasonFromRag != null && !topReasonFromRag.isBlank()) ? topReasonFromRag : s.reason;
            responses.add(new MemeRecommendationResponse(s.meme.getId(), s.meme.getTitle(), s.meme.getImgUrl(), reasonOut));
        }

        long tEnd = System.nanoTime();
        if (evalLogEnabled) {
            log.info("[SearchEval] query='{}', expanded='{}', total={}ms", qIn, normKeywordQuery, msBetween(t0, tEnd));
        }
        return responses;
    }

    private static long msBetween(long tStart, long tEnd) { return (tEnd - tStart) / 1_000_000L; }

    private double keywordMatchScore(String qLower, List<String> qTokens, Meme m) {
        String usage = safeLower(m.getUsageContext());
        String title = safeLower(m.getTitle());
        String origin = safeLower(m.getOrigin());
        List<String> tags = HashtagParser.parseHashtags(m.getHashtags()).stream()
            .map(s -> s.replace("#", "").toLowerCase()).filter(s -> !s.isBlank()).toList();

        int qSize = Math.max(1, qTokens.size());
        int usageMatches = countContainsTokens(usage, qTokens);
        int titleMatches = countContainsTokens(title, qTokens);
        int originMatches = countContainsTokens(origin, qTokens);
        int tagMatches = countTagMatches(tags, qTokens);

        // 전체 쿼리 구문이 포함되는지에 대한 보너스 점수
        boolean phraseMatch = usage.contains(qLower) || title.contains(qLower);

        double score = 0.0;
        score += 0.55 * (usageMatches / (double) qSize);
        score += 0.30 * (tagMatches / (double) qSize);
        score += 0.10 * (titleMatches / (double) qSize);
        score += 0.05 * (originMatches / (double) qSize);
        if (phraseMatch) score += 0.10;

        return Math.max(0.0, score);
    }

    private List<Scored> mmr(List<Scored> items, List<String> qTokens, int k) {
        if (items.isEmpty() || k <= 0) return List.of();
        items.sort((a,b) -> Double.compare(b.score, a.score));

        Map<Long, Set<String>> docTokens = new HashMap<>();
        for (Scored s : items) {
            docTokens.put(s.meme.getId(), new HashSet<>(tokenize(safeLower(s.meme.getTitle() + " " + s.meme.getUsageContext() + " " + s.meme.getHashtags()))));
        }

        List<Scored> selected = new ArrayList<>();
        List<Scored> candidates = new ArrayList<>(items);

        if (!candidates.isEmpty()) {
            selected.add(candidates.remove(0));
        }

        while (!candidates.isEmpty() && selected.size() < k) {
            Scored bestNext = null;
            double bestMmrScore = -Double.MAX_VALUE;

            for (Scored cand : candidates) {
                double relevance = cand.score;
                double maxSimilarity = 0.0;
                for (Scored sel : selected) {
                    maxSimilarity = Math.max(maxSimilarity, jaccard(docTokens.get(cand.meme.getId()), docTokens.get(sel.meme.getId())));
                }
                double mmrScore = mmrLambda * relevance - (1 - mmrLambda) * maxSimilarity;
                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore;
                    bestNext = cand;
                }
            }
            if (bestNext == null) break;
            selected.add(bestNext);
            candidates.remove(bestNext);
        }
        return selected;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private List<Scored> heuristicRerank(String normQuery, List<Scored> list) {
        return list.stream()
            .map(s -> {
                double bonus = 0.0;
                String usage = safeLower(s.meme.getUsageContext());
                if (usage.contains(normQuery)) bonus += 0.08;
                return new Scored(s.meme, s.score + bonus, s.reason);
            })
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .toList();
    }

    private static class Scored {
        final Meme meme; final double score; final String reason;
        Scored(Meme meme, double score, String reason) { this.meme = meme; this.score = score; this.reason = reason; }
    }

    private record ScoreResult(double score, String reason) {}

    private ScoreResult scoreAndExplain(String qLower, List<String> qTokens, Meme m) {
        String usage = safeLower(m.getUsageContext());
        String title = safeLower(m.getTitle());
        List<String> tags = HashtagParser.parseHashtags(m.getHashtags()).stream()
            .map(s -> s.replace("#", "").toLowerCase()).filter(s -> !s.isBlank()).toList();

        List<String> matchedTokens = new ArrayList<>();
        for (String token : qTokens) {
            if (title.contains(token) || usage.contains(token) || tags.stream().anyMatch(tag -> tag.contains(token))) {
                matchedTokens.add(token);
            }
        }

        List<String> reasons = new ArrayList<>();
        if (!matchedTokens.isEmpty()) {
            if (title.contains(qLower) || usage.contains(qLower)) {
                reasons.add("내용에 검색어가 직접 등장");
            } else {
                String keywords = matchedTokens.stream().distinct().limit(2).collect(Collectors.joining(", "));
                reasons.add("키워드 '" + keywords + "' 연관");
            }
        }

        List<String> matchedTags = findMatchedTags(tags, qTokens).stream().limit(2).toList();
        if (!matchedTags.isEmpty()) {
            reasons.add("해시태그 " + matchedTags.stream().map(t -> "#" + t).collect(Collectors.joining(", ")));
        }

        if (reasons.isEmpty()) {
            reasons.add("벡터 의미 유사도 기반 추천");
        }

        String reason = String.join(" · ", reasons);
        return new ScoreResult(0.0, reason); // Score calculation is now in keywordMatchScore
    }

    private static int countContainsTokens(String text, List<String> tokens) {
        if (text == null || text.isBlank() || tokens.isEmpty()) return 0;
        int c = 0;
        for (String t : tokens) if (text.contains(t)) c++;
        return c;
    }

    private static int countTagMatches(List<String> tags, List<String> tokens) {
        if (tags.isEmpty() || tokens.isEmpty()) return 0;
        Set<String> tokenSet = new HashSet<>(tokens);
        return (int) tags.stream().filter(tokenSet::contains).count();
    }

    private static List<String> findMatchedTags(List<String> tags, List<String> tokens) {
        Set<String> tokenSet = new HashSet<>(tokens);
        return tags.stream().filter(tokenSet::contains).toList();
    }

    private static List<String> tokenize(String s) {
        if (s == null) return List.of();
        return Arrays.stream(TOKEN_SPLIT.split(s.toLowerCase()))
            .filter(tok -> !tok.isBlank() && !STOP_WORDS.contains(tok)) // 불용어 제거
            .toList();
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}