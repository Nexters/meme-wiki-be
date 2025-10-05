package spring.memewikibe.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverRagService {

    @Value("${NAVER_AI_API_KEY:}")
    private String naverApiKey;
    @Value("${NAVER_AI_REQUEST_ID:meme-wiki-backend}")
    private String naverRequestId;
    @Value("${NAVER_AI_API_ENDPOINT:https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003}")
    private String naverApiEndpoint;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void printApiKey() {
        log.info("NAVER_AI_API_KEY loaded: {}", isConfigured() ? "******" + naverApiKey.substring(Math.max(0, naverApiKey.length() - 4)) : "NOT SET");
    }

    private static final String SYSTEM_PROMPT =
        "너는 오직 주어진 텍스트 정보에만 근거하여 검색어와 밈(Meme)의 관련성을 판단하는 극도로 논리적이고 정직한 분석 AI다.\n" +
            "너의 임무는 아래 **[분석 단계]**를 반드시 순서대로 수행하고, 그 결과를 바탕으로 최종 결론을 내리는 것이다.\n\n" +
            "**[엄격한 규칙]**\n" +
            "1. **연상 작용 절대 금지:** 너의 사전 지식이나 창의적 연상(예: '스트레스'->'커피', '퇴사'->'아메리카노')을 통한 확장 해석은 1%도 허용되지 않는다.\n" +
            "2. **판단 기준:** 관련성 판단은 오직 '문자열의 직접 포함' 또는 '단어의 명백한 동의어/유의어 관계'에만 근거해야 한다.\n" +
            "3. **단계별 사고:** 반드시 아래 [분석 단계]를 따라서 사고하고 그 과정을 `thought` 필드에 기록해야 한다.\n\n" +
            "**[분석 단계]**\n" +
            "1. 사용자의 `검색어`(예: '회사그만둬야지')에서 핵심 의미(예: '퇴사')를 파악한다.\n" +
            "2. 각 `후보 밈`의 `제목`, `사용 맥락`, `해시태그`를 확인한다.\n" +
            "3. 1단계에서 파악한 핵심 의미와 관련된 단어(예: '퇴사', '사직', '퇴사짤')가 2단계의 텍스트에 **직접 포함되거나, 의미가 매우 유사한 동의어/유의어가 포함**되는지 확인한다.\n" +
            "4. 관련 단어가 포함된 밈이 있다면, 가장 관련성이 높은 밈을 선택한다. 없다면, '직접적인 관련성을 찾지 못함'으로 판단한다.\n\n" +
            "**[출력 형식]**\n" +
            "아래의 JSON 형식을 반드시 준수하여 응답해라. 다른 설명은 절대 추가하지 마라.\n" +
            "{\n" +
            "  \"thought\": \"여기에 [분석 단계]에 따른 너의 단계별 사고 과정을 상세히 서술하라. 예: 1. 검색어 '회사그만둬야지'의 핵심 의미는 '퇴사'이다. 2. 후보 16번 밈의 제목에 '퇴사짤'이 포함되어 있다. 3. 따라서 후보 16번이 가장 적합하다.\",\n" +
            "  \"best_meme_id\": [최종 선택한 밈의 ID (정수)],\n" +
            "  \"reason\": \"[최종 선택한 이유 (문자열)]\"\n" +
            "}";

    public record Candidate(long id, String title, String usageContext, String hashtags) {}
    public record RagResult(List<Long> ids, String reason) {}

    public RagResult recommendWithContextDetailed(String userContext, String query, List<Candidate> candidates) {
        if (!isConfigured()) {
            log.warn("NAVER_AI_API_KEY is not configured. Using fallback.");
            return createFallbackResult(query, candidates);
        }

        if (candidates.isEmpty()) {
            return new RagResult(List.of(), "추천할 밈이 없습니다.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + naverApiKey);
            headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", naverRequestId);

            HttpEntity<Map<String, Object>> requestEntity = buildRequestEntity(headers, query, candidates);
            String responseStr = restTemplate.postForObject(naverApiEndpoint, requestEntity, String.class);
            log.debug("LLM Raw Response: {}", responseStr);

            return parseLlmResponse(responseStr, candidates);

        } catch (Exception e) {
            log.error("Error during Naver HyperCLOVA X API call or parsing. Falling back.", e);
            return createFallbackResult(query, candidates);
        }
    }

    private boolean isConfigured() {
        return naverApiKey != null && !naverApiKey.isBlank();
    }

    private RagResult parseLlmResponse(String responseStr, List<Candidate> candidates) throws JsonProcessingException {
        Map<String, Object> responseJson = objectMapper.readValue(responseStr, Map.class);

        Map<String, Object> resultObject = (Map<String, Object>) responseJson.get("result");
        if (resultObject == null) {
            log.warn("LLM response does not contain 'result' field. Falling back. Response: {}", responseStr);
            return createFallbackResult(null, candidates);
        }

        Map<String, Object> messageObject = (Map<String, Object>) resultObject.get("message");
        if (messageObject == null) {
            log.warn("LLM result does not contain 'message' field. Falling back. Response: {}", responseStr);
            return createFallbackResult(null, candidates);
        }

        String llmContent = (String) messageObject.get("content");
        if (llmContent == null || llmContent.isBlank()) {
            log.warn("LLM message content is empty. Falling back. Response: {}", responseStr);
            return createFallbackResult(null, candidates);
        }

        // AI 응답 문자열을 강력하게 정제하는 로직
        String extractedJson = llmContent;
        if (extractedJson.contains("```json")) {
            extractedJson = extractedJson.substring(extractedJson.indexOf('{'), extractedJson.lastIndexOf('}') + 1);
        }
        extractedJson = extractedJson.trim();
        if (extractedJson.startsWith("\"") && extractedJson.endsWith("\"")) {
            extractedJson = extractedJson.substring(1, extractedJson.length() - 1);
        }
        extractedJson = extractedJson.replace("\\\"", "\"").replace("\\\\", "\\");
        extractedJson = extractedJson.replaceAll("\\s*\\n\\s*", " ").trim();

        Map<String, Object> llmOutput = objectMapper.readValue(extractedJson, Map.class);

        Object bestIdObject = llmOutput.get("best_meme_id");
        if (bestIdObject == null) {
            log.info("LLM did not select a best meme (likely no relevant candidates). Returning original ranking.");
            return createFallbackResult(null, candidates);
        }

        Long bestId = ((Number) bestIdObject).longValue();
        String reason = (String) llmOutput.get("reason");
        List<Long> reorderedIds = reorderCandidates(candidates, bestId);

        return new RagResult(reorderedIds, reason);
    }

    private RagResult createFallbackResult(String query, List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return new RagResult(List.of(), "추천할 밈이 없습니다.");
        }
        List<Long> ids = candidates.stream().map(Candidate::id).toList();
        return new RagResult(ids, synthesizeReasonFallback(query, candidates.get(0)));
    }

    private HttpEntity<Map<String, Object>> buildRequestEntity(HttpHeaders headers, String query, List<Candidate> candidates) {
        String userMessage = buildUserMessage(query, candidates);
        Map<String, Object> requestBody = Map.of(
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userMessage)
            ),
            "topP", 0.8, "topK", 0, "maxTokens", 512, "temperature", 0.2
        );
        return new HttpEntity<>(requestBody, headers);
    }

    private String buildUserMessage(String query, List<Candidate> candidates) {
        String candidatesStr = candidates.stream()
            .map(c -> String.format("- ID: %d, 제목: %s, 사용 맥락: %s, 해시태그: %s",
                c.id(), c.title(), c.usageContext(), c.hashtags()))
            .collect(Collectors.joining("\n"));
        return String.format("**[입력 정보]**\n- 검색어: %s\n- 후보 밈 목록:\n%s", query, candidatesStr);
    }

    private List<Long> reorderCandidates(List<Candidate> candidates, long bestId) {
        if (candidates.stream().noneMatch(c -> c.id() == bestId)) {
            return candidates.stream().map(Candidate::id).toList();
        }
        List<Long> reordered = new ArrayList<>();
        reordered.add(bestId);
        candidates.forEach(c -> {
            if (c.id() != bestId) {
                reordered.add(c.id());
            }
        });
        return reordered;
    }

    private String synthesizeReasonFallback(String query, Candidate topCandidate) {
        if (topCandidate == null) {
            return "쿼리와 관련된 밈을 찾지 못했어요.";
        }
        if (query == null) {
            return "의미적으로 유사하여 추천되었어요.";
        }

        String title = topCandidate.title() != null ? topCandidate.title().toLowerCase() : "";
        String usage = topCandidate.usageContext() != null ? topCandidate.usageContext().toLowerCase() : "";
        String tags = topCandidate.hashtags() != null ? topCandidate.hashtags().toLowerCase() : "";
        String qLower = query.toLowerCase();

        if (title.contains(qLower)) {
            return "밈 제목에 검색어가 직접 포함되어 있어요.";
        }
        if (usage.contains(qLower)) {
            return "사용되는 맥락이 검색어와 관련이 깊어요.";
        }
        if (tags.contains(qLower)) {
            return "관련 해시태그를 포함하고 있어요.";
        }
        return "의미적으로 유사하여 추천되었어요.";
    }
}