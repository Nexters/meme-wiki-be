package spring.memewikibe.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import spring.memewikibe.config.NaverAiProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NaverQueryRewriter 단위 테스트")
class NaverQueryRewriterTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private NaverAiProperties naverAiProperties;
    private NaverQueryRewriter queryRewriter;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_REQUEST_ID = "test-request-id";
    private static final String TEST_ENDPOINT = "https://test.api.endpoint";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        naverAiProperties = new NaverAiProperties();
        naverAiProperties.setApiKey(TEST_API_KEY);
        naverAiProperties.setRequestId(TEST_REQUEST_ID);
        naverAiProperties.setApiEndpoint(TEST_ENDPOINT);

        queryRewriter = new NaverQueryRewriter(restTemplate, objectMapper, naverAiProperties);
    }

    @Test
    @DisplayName("rewrite: 원본 쿼리를 그대로 반환한다")
    void rewrite_returnsOriginalQuery() {
        // given
        String query = "회사 그만두다";
        String userContext = "some context";

        // when
        String result = queryRewriter.rewrite(userContext, query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("rewrite: null 쿼리를 그대로 반환한다")
    void rewrite_returnsNullQuery() {
        // when
        String result = queryRewriter.rewrite(null, null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("expandForKeywords: 정상적으로 키워드를 확장한다")
    void expandForKeywords_succeeds_withValidResponse() throws Exception {
        // given
        String query = "회사 그만두다";
        String expectedKeywords = "회사 그만두다 퇴사 사직";
        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": "%s"
                    }
                }
            }
            """.formatted(expectedKeywords);

        when(restTemplate.postForObject(
            eq(TEST_ENDPOINT),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(mockResponse);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(expectedKeywords);
        verify(restTemplate).postForObject(eq(TEST_ENDPOINT), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("expandForKeywords: API 키가 없으면 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenApiKeyIsBlank() {
        // given
        naverAiProperties.setApiKey("");
        String query = "테스트 쿼리";

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("expandForKeywords: API 키가 null이면 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenApiKeyIsNull() {
        // given
        naverAiProperties.setApiKey(null);
        String query = "테스트 쿼리";

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("expandForKeywords: null 쿼리는 그대로 반환한다")
    void expandForKeywords_returnsNull_whenQueryIsNull() {
        // when
        String result = queryRewriter.expandForKeywords(null);

        // then
        assertThat(result).isNull();
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("expandForKeywords: 빈 쿼리는 그대로 반환한다")
    void expandForKeywords_returnsBlank_whenQueryIsBlank() {
        // given
        String query = "   ";

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("expandForKeywords: API 응답이 null이면 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenResponseIsNull() {
        // given
        String query = "테스트";
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(null);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("expandForKeywords: API 호출 실패 시 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenApiCallFails() {
        // given
        String query = "테스트";
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenThrow(new RestClientException("API call failed"));

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("expandForKeywords: 잘못된 JSON 응답 시 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenResponseIsInvalidJson() {
        // given
        String query = "테스트";
        String invalidJson = "{ invalid json }";
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(invalidJson);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("expandForKeywords: result 필드가 없는 응답 시 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenResponseMissingResultField() {
        // given
        String query = "테스트";
        String jsonWithoutResult = """
            {
                "error": "something"
            }
            """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(jsonWithoutResult);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("expandForKeywords: message 필드가 없는 응답 시 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenResponseMissingMessageField() {
        // given
        String query = "테스트";
        String jsonWithoutMessage = """
            {
                "result": {
                    "something": "else"
                }
            }
            """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(jsonWithoutMessage);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("expandForKeywords: content 필드가 없는 응답 시 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery_whenResponseMissingContentField() {
        // given
        String query = "테스트";
        String jsonWithoutContent = """
            {
                "result": {
                    "message": {
                        "role": "assistant"
                    }
                }
            }
            """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(jsonWithoutContent);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("expandForKeywords: 키워드 앞뒤 공백을 제거한다")
    void expandForKeywords_trimsWhitespace() throws Exception {
        // given
        String query = "테스트";
        String keywordsWithSpaces = "  키워드1 키워드2  ";
        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": "%s"
                    }
                }
            }
            """.formatted(keywordsWithSpaces);

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(mockResponse);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo("키워드1 키워드2");
    }

    @Test
    @DisplayName("expandForKeywords: 올바른 헤더를 포함하여 API를 호출한다")
    void expandForKeywords_sendsCorrectHeaders() throws Exception {
        // given
        String query = "테스트 쿼리";
        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": "키워드"
                    }
                }
            }
            """;

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(
            eq(TEST_ENDPOINT),
            entityCaptor.capture(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // when
        queryRewriter.expandForKeywords(query);

        // then
        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getHeaders().getContentType().toString())
            .isEqualTo("application/json");
        assertThat(capturedEntity.getHeaders().get("Authorization"))
            .containsExactly("Bearer " + TEST_API_KEY);
        assertThat(capturedEntity.getHeaders().get("X-NCP-CLOVASTUDIO-REQUEST-ID"))
            .containsExactly(TEST_REQUEST_ID);
    }

    @Test
    @DisplayName("expandForKeywords: 올바른 요청 본문을 전송한다")
    void expandForKeywords_sendsCorrectRequestBody() throws Exception {
        // given
        String query = "테스트 쿼리";
        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": "키워드"
                    }
                }
            }
            """;

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(
            eq(TEST_ENDPOINT),
            entityCaptor.capture(),
            eq(String.class)
        )).thenReturn(mockResponse);

        // when
        queryRewriter.expandForKeywords(query);

        // then
        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) capturedEntity.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("topP")).isEqualTo(0.8);
        assertThat(body.get("topK")).isEqualTo(0);
        assertThat(body.get("maxTokens")).isEqualTo(60);
        assertThat(body.get("temperature")).isEqualTo(0.1);
    }
}
