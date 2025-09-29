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

    // Backward-compatible simple path used by /api/recommendations/search
    public List<Long> recommendWithContext(String userContext, String query, List<Long> candidateIds) {
        if (!isConfigured()) {
            log.warn("Naver AI Studio API key missing. Returning vector candidates.");
            return candidateIds;
        }
        return candidateIds; // TODO: call NAVER RAG and reorder
    }

    public static record Candidate(long id, String title, String usageContext, String hashtags) {}
    public static record RagResult(List<Long> ids, String reason) {}

    // New detailed path used by search-explain: accepts rich candidates and returns order + witty reason for top-1
    public RagResult recommendWithContextDetailed(String userContext, String query, List<Candidate> candidates) {
        if (!isConfigured()) {
            // Preserve incoming order and synthesize a short reason.
            List<Long> ids = candidates.stream().map(Candidate::id).toList();
            String reason = synthesizeReason(query, candidates);
            return new RagResult(ids, reason);
        }
        // TODO: Implement NAVER HyperCLOVA X call with a system prompt like:
        // "당신은 한국 인터넷 밈 동향을 완벽하게 이해하는 전문가입니다. 사용자의 쿼리 의도와 문화적 맥락을 파악하여,\n" +
        // "주어진 밈 후보 목록 중에서 가장 적절한 순서로 재배열하고, 가장 좋은 추천 1개에 대한 재치 있는 추천 사유를 한 문장으로 생성해주세요."
        List<Long> ids = candidates.stream().map(Candidate::id).toList();
        String reason = synthesizeReason(query, candidates);
        return new RagResult(ids, reason);
    }

    private boolean isConfigured() {
        return naverApiKey != null && !naverApiKey.isBlank();
    }

    private String synthesizeReason(String query, List<Candidate> candidates) {
        if (candidates.isEmpty()) return "쿼리와 관련된 밈을 찾지 못했어요.";
        Candidate top = candidates.get(0);
        String title = top.title() == null ? "이 밈" : top.title();
        return String.format("\"%s\" 맥락에 %s이(가) 딱 맞아요 — 해시태그와 사용 맥락이 유사해요!", query, title);
    }
}
