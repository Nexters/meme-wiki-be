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
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NaverQueryRewriter 단위 테스트")
class NaverQueryRewriterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NaverAiProperties naverAiProperties;

    private NaverQueryRewriter queryRewriter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        queryRewriter = new NaverQueryRewriter(restTemplate, objectMapper, naverAiProperties);
    }

    @Test
    @DisplayName("rewrite 메소드는 원본 쿼리를 그대로 반환한다")
    void rewrite_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("API 키가 없으면 원본 쿼리를 반환한다")
    void expandForKeywords_noApiKey_returnsOriginalQuery() {
        // given
        String query = "회사 그만두고 싶다";
        when(naverAiProperties.getApiKey()).thenReturn("");

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("null 쿼리에 대해 빈 문자열을 반환한다")
    void expandForKeywords_nullQuery_returnsEmptyString() {
        // when
        String result = queryRewriter.expandForKeywords(null);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("빈 쿼리에 대해 빈 문자열을 반환한다")
    void expandForKeywords_blankQuery_returnsBlankString() {
        // when
        String result = queryRewriter.expandForKeywords("   ");

        // then
        assertThat(result).isEqualTo("   ");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("정상적인 API 응답을 파싱하여 확장된 키워드를 반환한다")
    void expandForKeywords_validResponse_returnsExpandedKeywords() {
        // given
        String query = "회사 그만두고 싶다";
        String apiKey = "test-api-key";
        String requestId = "test-request-id";
        String endpoint = "https://api.test.com";

        when(naverAiProperties.getApiKey()).thenReturn(apiKey);
        when(naverAiProperties.getRequestId()).thenReturn(requestId);
        when(naverAiProperties.getApiEndpoint()).thenReturn(endpoint);

        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": "회사 그만두다 퇴사 사직"
                    }
                }
            }
            """;

        when(restTemplate.postForObject(eq(endpoint), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponse);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo("회사 그만두다 퇴사 사직");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq(endpoint), entityCaptor.capture(), eq(String.class));

        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer " + apiKey);
        assertThat(headers.getFirst("X-NCP-CLOVASTUDIO-REQUEST-ID")).isEqualTo(requestId);
    }

    @Test
    @DisplayName("API 호출 실패 시 원본 쿼리를 반환한다")
    void expandForKeywords_apiCallFails_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RestClientException("Network error"));

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("빈 응답을 받으면 원본 쿼리를 반환한다")
    void expandForKeywords_emptyResponse_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("");

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("null 응답을 받으면 원본 쿼리를 반환한다")
    void expandForKeywords_nullResponse_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(null);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("잘못된 JSON 응답을 받으면 원본 쿼리를 반환한다")
    void expandForKeywords_malformedJson_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn("invalid json");

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("응답 구조가 예상과 다르면 원본 쿼리를 반환한다")
    void expandForKeywords_unexpectedStructure_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        // Missing 'message' field
        String mockResponse = """
            {
                "result": {
                    "wrong_field": "value"
                }
            }
            """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponse);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("content가 null인 응답을 받으면 원본 쿼리를 반환한다")
    void expandForKeywords_nullContent_returnsOriginalQuery() {
        // given
        String query = "테스트 쿼리";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": null
                    }
                }
            }
            """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponse);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo(query);
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 키워드를 trim하여 반환한다")
    void expandForKeywords_contentWithSpaces_returnsTrimmed() {
        // given
        String query = "테스트";
        when(naverAiProperties.getApiKey()).thenReturn("test-key");
        when(naverAiProperties.getRequestId()).thenReturn("test-id");
        when(naverAiProperties.getApiEndpoint()).thenReturn("https://api.test.com");

        String mockResponse = """
            {
                "result": {
                    "message": {
                        "content": "  키워드1 키워드2  "
                    }
                }
            }
            """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponse);

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo("키워드1 키워드2");
    }
}
