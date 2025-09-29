package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Lightweight keyword search over DB fields (title, hashtags, origin, usageContext)
 * returning normalized scores for hybrid fusion.
 */
@Service
@RequiredArgsConstructor
public class SimpleKeywordSearchService implements MemeVectorIndexService.KeywordSearchService {

    private final MemeRepository memeRepository;

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s\\p{Punct}]+");

    @Override
    public List<MemeVectorIndexService.SearchHit> searchWithScores(String query, int topK) {
        if (!StringUtils.hasText(query)) return List.of();
        String qLower = query.toLowerCase(Locale.ROOT);
        List<String> qTokens = tokenize(qLower);
        if (qTokens.isEmpty()) return List.of();
        List<Meme> candidates = memeRepository.findKeywordCandidatesAcrossFields(qLower, org.springframework.data.domain.Limit.of(topK));
        if (candidates.isEmpty()) return List.of();

        List<Scored> scored = new ArrayList<>(candidates.size());
        double max = 0.0;
        for (Meme m : candidates) {
            double s = keywordMatchScore(qLower, qTokens, m);
            max = Math.max(max, s);
            scored.add(new Scored(m.getId(), s));
        }
        // normalize 0..1
        double norm = max <= 0 ? 1.0 : max;
        List<MemeVectorIndexService.SearchHit> out = new ArrayList<>(scored.size());
        for (Scored sc : scored) {
            out.add(new MemeVectorIndexService.SearchHit(sc.id, sc.score / norm, "sparse"));
        }
        out.sort(Comparator.comparingDouble(MemeVectorIndexService.SearchHit::score).reversed());
        if (out.size() > topK) return new ArrayList<>(out.subList(0, topK));
        return out;
    }

    private record Scored(Long id, double score) {}

    private static List<String> tokenize(String s) {
        return Arrays.stream(TOKEN_SPLIT.split(s))
            .filter(tok -> !tok.isBlank())
            .toList();
    }

    private static double keywordMatchScore(String qLower, List<String> qTokens, Meme m) {
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

    private static String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }
}
