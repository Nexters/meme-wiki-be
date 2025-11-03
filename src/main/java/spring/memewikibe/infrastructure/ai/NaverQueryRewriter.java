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
import spring.memewikibe.config.NaverAiProperties;

import java.util.List;
import java.util.Map;

/**
 * Naver Clova Studio-based implementation of QueryRewriter.
 * Uses Naver AI API to extract keywords and synonyms from user queries for enhanced search.
 * This implementation is marked as @Primary and will be used by default when QueryRewriter is injected.
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

    /**
     * Returns the original query without rewriting.
     * This method currently does not perform sentence-level rewriting.
     *
     * @param userContext the user context (not currently used)
     * @param query the original search query
     * @return the original query unchanged
     */
    @Override
    public String rewrite(String userContext, String query) {
        return query;
    }

    /**
     * Expands a search query into keywords and synonyms using Naver Clova Studio API.
     * Extracts 1-3 core keywords and 1-2 related synonyms to improve search results.
     * Falls back to the original query if the API call fails or configuration is missing.
     *
     * @param query the original search query
     * @return space-separated keywords and synonyms, or the original query if expansion fails
     */
    @Override
    public String expandForKeywords(String query) {
        if (query == null || query.isBlank() ||
            naverAiProperties.getApiKey() == null || naverAiProperties.getApiKey().isBlank()) {
            return query;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + naverAiProperties.getApiKey());
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

            if (responseStr == null) {
                log.warn("Received null response from Naver AI API. Falling back to original query.");
                return query;
            }

            String expandedKeywords = extractKeywordsFromResponse(responseStr);
            log.info("Query '{}' expanded to keywords: '{}'", query, expandedKeywords);
            return expandedKeywords.trim();

        } catch (Exception e) {
            log.error("Failed to expand keywords for query: '{}'. Falling back to original query.", query, e);
            return query;
        }
    }

    /**
     * Extracts the keyword content from the Naver AI API response JSON.
     * Performs safe navigation through the nested JSON structure.
     *
     * @param responseStr the raw JSON response string
     * @return the extracted keywords content
     * @throws Exception if the response structure is invalid or content cannot be extracted
     */
    @SuppressWarnings("unchecked")
    private String extractKeywordsFromResponse(String responseStr) throws Exception {
        Map<String, Object> responseJson = objectMapper.readValue(responseStr, Map.class);

        Map<String, Object> resultObject = (Map<String, Object>) responseJson.get("result");
        if (resultObject == null) {
            throw new IllegalStateException("Response missing 'result' field");
        }

        Map<String, Object> messageObject = (Map<String, Object>) resultObject.get("message");
        if (messageObject == null) {
            throw new IllegalStateException("Response missing 'message' field");
        }

        String content = (String) messageObject.get("content");
        if (content == null) {
            throw new IllegalStateException("Response missing 'content' field");
        }

        return content;
    }
}