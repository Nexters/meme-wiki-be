package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * NAVER-aware query rewriter. Currently a lightweight heuristic/stub that
 * expands a few common Korean intents. If NAVER key is configured, we log
 * that LLM-based rewrite would be invoked; for now, we still return the
 * heuristic-expanded query to keep the code path deterministic in CI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class NaverQueryRewriter implements QueryRewriter {

    @Value("${NAVER_AI_API_KEY:}")
    private String naverApiKey;

    private static final Map<String, String> HEURISTIC_EXPANSIONS = new HashMap<>();
    static {
        // Simple Korean intent expansions; can be replaced by LLM rewriting later.
        HEURISTIC_EXPANSIONS.put("퇴근하고싶다", "퇴근 귀가 집가고싶다 피곤 지침");
        HEURISTIC_EXPANSIONS.put("집에 가고싶다", "집가고싶다 귀가 퇴근 집 집으로");
        HEURISTIC_EXPANSIONS.put("현타", "현타 허무 허탈 공허 무기력");
        HEURISTIC_EXPANSIONS.put("억까", "억까 부당 비난 악플");
    }

    @Override
    public String rewrite(String userContext, String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) return "";
        String expanded = HEURISTIC_EXPANSIONS.getOrDefault(trimmed, trimmed);
        if (isConfigured()) {
            // Placeholder: In production, call NAVER HyperCLOVA X to rewrite and expand.
            log.debug("[QueryRewrite] NAVER configured, would LLM-rewrite: '{}' -> '{}' (user={})", trimmed, expanded, userContext);
        } else {
            log.debug("[QueryRewrite] NAVER not configured, heuristic rewrite: '{}' -> '{}'", trimmed, expanded);
        }
        return expanded;
    }

    private boolean isConfigured() {
        return naverApiKey != null && !naverApiKey.isBlank();
    }
}
