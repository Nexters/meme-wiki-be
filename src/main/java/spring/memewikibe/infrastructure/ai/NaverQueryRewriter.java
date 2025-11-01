package spring.memewikibe.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Naver AI-powered query rewriter for semantic search enhancement.
 * This implementation uses Naver Clova Studio API to expand and rewrite user queries.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class NaverQueryRewriter implements QueryRewriter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NaverAiProperties naverAiProperties;

    private static final String KEYWORD_EXTRACTION_PROMPT =
        "사용자의 검색어에서 검색에 사용할 핵심 키워드를 1~3개 추출하고, 관련된 동의어나 유의어를 1~2개 추가하여 공백으로 구분된 목록으로 반환하라. " +
            "오직 키워드 목록만 반환하고 다른 설명은 절대 추가하지 마라.\n\n" +
            "예시 1:\n" +
            "입력: 회사를그만두는게맞을거같군\n" +
            "출력: 회사 그만두다 퇴사 사직\n\n" +
            "예시 2:\n" +
            "입력: 오늘 기분 최고다\n" +
            "출력: 기분 최고 행복 기쁨\n\n" +
            "예시 3:\n" +
            "입력: 시험 망했다ㅠㅠ\n" +
            "출력: 시험 망했다 슬픔 좌절";

    @Override
    public String rewrite(String userContext, String query) {
        return query;
    }

    @Override
    public String expandForKeywords(String query) {
        if (query == null || query.isBlank()) {
            return query != null ? query : "";
        }

        String apiKey = naverAiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Naver AI API key not configured, returning original query");
            return query;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", naverAiProperties.getRequestId());

            Map<String, Object> requestBody = Map.of(
                "messages", List.of(
                    Map.of("role", "system", "content", KEYWORD_EXTRACTION_PROMPT),
                    Map.of("role", "user", "content", query)
                ),
                "topP", 0.8,
                "topK", 0,
                "maxTokens", 60,
                "temperature", 0.1
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            String responseStr = restTemplate.postForObject(
                naverAiProperties.getApiEndpoint(),
                requestEntity,
                String.class
            );

            if (responseStr == null || responseStr.isBlank()) {
                log.warn("Received empty response from Naver AI API");
                return query;
            }

            String expandedKeywords = extractKeywordsFromResponse(responseStr);
            if (expandedKeywords == null || expandedKeywords.isEmpty()) {
                log.warn("Failed to extract keywords from response, returning original query");
                return query;
            }
            log.info("Query '{}' expanded to keywords: '{}'", query, expandedKeywords);
            return expandedKeywords;

        } catch (Exception e) {
            log.error("Failed to expand keywords for query: '{}'. Falling back to original query.", query, e);
            return query;
        }
    }

    /**
     * Extracts keywords from the Naver AI API response with proper null safety checks.
     *
     * @param responseStr JSON response from Naver AI API
     * @return extracted keywords, or empty string if extraction fails
     */
    private String extractKeywordsFromResponse(String responseStr) {
        try {
            Map<String, Object> responseJson = objectMapper.readValue(responseStr, Map.class);

            if (responseJson == null) {
                return "";
            }

            Object resultObj = responseJson.get("result");
            if (!(resultObj instanceof Map)) {
                log.warn("Unexpected response structure: 'result' is not a Map");
                return "";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resultObject = (Map<String, Object>) resultObj;

            Object messageObj = resultObject.get("message");
            if (!(messageObj instanceof Map)) {
                log.warn("Unexpected response structure: 'message' is not a Map");
                return "";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> messageObject = (Map<String, Object>) messageObj;

            Object contentObj = messageObject.get("content");
            if (contentObj == null) {
                log.warn("Unexpected response structure: 'content' is null");
                return "";
            }

            return contentObj.toString().trim();

        } catch (Exception e) {
            log.error("Failed to parse Naver AI API response", e);
            return "";
        }
    }
}