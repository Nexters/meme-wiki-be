package spring.memewikibe.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary; // [추가] @Primary 어노테이션 import
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary // [추가] 여러 QueryRewriter 구현체 중 이 클래스를 우선적으로 사용하도록 설정
@RequiredArgsConstructor
public class NaverQueryRewriter implements QueryRewriter {

    // ... (이하 모든 코드는 이전과 동일)

    @Value("${NAVER_AI_API_KEY:}")
    private String naverApiKey;
    @Value("${NAVER_AI_REQUEST_ID:meme-wiki-qrewrite}")
    private String naverRequestId;
    @Value("${NAVER_AI_API_ENDPOINT:https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003}")
    private String naverApiEndpoint;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
        if (query == null || query.isBlank() || naverApiKey == null || naverApiKey.isBlank()) {
            return query;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + naverApiKey);
            headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", naverRequestId);

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
            String responseStr = restTemplate.postForObject(naverApiEndpoint, requestEntity, String.class);

            Map<String, Object> responseJson = objectMapper.readValue(responseStr, Map.class);
            Map<String, Object> resultObject = (Map<String, Object>) responseJson.get("result");
            Map<String, Object> messageObject = (Map<String, Object>) resultObject.get("message");
            String expandedKeywords = (String) messageObject.get("content");

            log.info("Query '{}' expanded to keywords: '{}'", query, expandedKeywords);
            return expandedKeywords.trim();

        } catch (Exception e) {
            log.error("Failed to expand keywords for query: '{}'. Falling back to original query.", query, e);
            return query;
        }
    }
}