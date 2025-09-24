package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stub service representing Naver AI Studio RAG orchestration.
 *
 * For now, it simply returns the ids provided (from vector results).
 * In production, you can call Naver AI Studio with the user profile context + retrieved documents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverRagService {

    @Value("${NAVER_AI_API_KEY:}")
    private String naverApiKey;

    public List<Long> recommendWithContext(String userContext, String query, List<Long> candidateIds) {
        // If not configured, just return candidates as-is.
        if (naverApiKey == null || naverApiKey.isBlank()) {
            log.warn("Naver AI Studio API key missing. Returning vector candidates.");
            return candidateIds;
        }
        // Placeholder: You can implement a call that re-ranks candidates using Naver AI Studio RAG.
        return candidateIds;
    }
}
