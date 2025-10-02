package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Lightweight keyword search over DB fields, now supporting intelligent keyword lists.
 */
@Service
@Primary // 여러 KeywordSearchService 구현체가 있을 경우, 이것을 우선 사용
@RequiredArgsConstructor
public class SimpleKeywordSearchService implements MemeVectorIndexService.KeywordSearchService {

    private final MemeRepository memeRepository;

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,]+");

    private record Scored(Long id, double score) {}

    /**
     * [기존 메소드 - 수정됨]
     * 하위 호환성을 위해 유지하되, 내부 로직은 새로운 List<String> 버전의 메소드에 위임합니다.
     */
    @Override
    public List<MemeVectorIndexService.SearchHit> searchWithScores(String query, int topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        // 받은 문자열 쿼리를 토큰화하여 새로운 메소드를 호출합니다.
        List<String> qTokens = tokenize(query.toLowerCase(Locale.ROOT));
        if (qTokens.isEmpty()) {
            return List.of();
        }
        return searchWithScores(qTokens, topK);
    }

    /**
     * [신규 핵심 메소드]
     * 여러 키워드 토큰을 받아, 지능화된 OR 검색을 수행하고 점수를 매깁니다.
     */
    @Override
    public List<MemeVectorIndexService.SearchHit> searchWithScores(List<String> keywords, int topK) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        // 우리가 MemeRepository에 추가한 강력한 OR 검색 메소드를 호출합니다.
        List<Meme> candidates = memeRepository.findKeywordCandidatesAcrossFields(keywords, org.springframework.data.domain.Limit.of(topK));
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Scored> scored = new ArrayList<>(candidates.size());
        double max = 0.0;
        String joinedKeywords = String.join(" ", keywords); // phrase matching을 위해 사용

        for (Meme m : candidates) {
            double s = keywordMatchScore(joinedKeywords, keywords, m);
            max = Math.max(max, s);
            scored.add(new Scored(m.getId(), s));
        }

        // 점수 정규화 (0..1)
        double norm = max <= 0 ? 1.0 : max;
        List<MemeVectorIndexService.SearchHit> out = new ArrayList<>(scored.size());
        for (Scored sc : scored) {
            out.add(new MemeVectorIndexService.SearchHit(sc.id, sc.score / norm, "sparse"));
        }

        out.sort(Comparator.comparingDouble(MemeVectorIndexService.SearchHit::score).reversed());

        if (out.size() > topK) {
            return out.subList(0, topK);
        }
        return out;
    }

    // --- Helper Methods (이전과 동일) ---

    private static List<String> tokenize(String s) {
        if (s == null) return List.of();
        return Arrays.stream(TOKEN_SPLIT.split(s.toLowerCase()))
            .filter(tok -> !tok.isBlank()).toList();
    }

    private static double keywordMatchScore(String qLower, List<String> qTokens, Meme m) {
        String usage = safeLower(m.getUsageContext());
        String title = safeLower(m.getTitle());
        String origin = safeLower(m.getOrigin());
        List<String> tags = HashtagParser.parseHashtags(m.getHashtags()).stream()
            .map(s -> s.replace("#", "").toLowerCase())
            .filter(s -> !s.isBlank())
            .toList();

        int qSize = Math.max(1, qTokens.size());
        int usageMatches = countContainsTokens(usage, qTokens);
        int titleMatches = countContainsTokens(title, qTokens);
        int originMatches = countContainsTokens(origin, qTokens);
        int tagMatches = countTagMatches(tags, qTokens);

        boolean phraseMatch = qTokens.size() > 1 && (usage.contains(qLower) || title.contains(qLower));

        double score = 0.0;
        score += 0.55 * (usageMatches / (double) qSize);
        score += 0.30 * (tagMatches / (double) qSize);
        score += 0.10 * (titleMatches / (double) qSize);
        score += 0.05 * (originMatches / (double) qSize);
        if (phraseMatch) score += 0.10;

        return Math.max(0.0, score);
    }

    private static int countContainsTokens(String text, List<String> tokens) {
        if (text == null || text.isBlank() || tokens.isEmpty()) return 0;
        int c = 0;
        for (String t : tokens) {
            if (text.contains(t)) c++;
        }
        return c;
    }

    private static int countTagMatches(List<String> tags, List<String> tokens) {
        if (tags.isEmpty() || tokens.isEmpty()) return 0;
        int c = 0;
        Set<String> tokenSet = new HashSet<>(tokens);
        for (String tag : tags) {
            if (tokenSet.contains(tag)) {
                c++;
            }
        }
        return c;
    }

    private static String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }
}