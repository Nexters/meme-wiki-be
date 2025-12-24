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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.infrastructure.ai.CrossEncoderReranker.Candidate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("NaverCrossEncoderReranker 단위 테스트")
class NaverCrossEncoderRerankerTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private ObjectMapper objectMapper;
    private NaverCrossEncoderReranker sut;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sut = new NaverCrossEncoderReranker(mockRestTemplate, objectMapper);

        // Set default config values
        ReflectionTestUtils.setField(sut, "naverApiKey", "test-api-key");
        ReflectionTestUtils.setField(sut, "naverRequestId", "test-request-id");
        ReflectionTestUtils.setField(sut, "rerankerApiEndpoint", "https://test.api.endpoint");
    }

    @Test
    @DisplayName("rerank: AI API가 문서를 재정렬하여 반환")
    void rerank_succeeds_withValidApiResponse() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.5)
        );

        String apiResponse = """
            {
              "result": {
                "citedDocuments": [
                  {"id": "3"},
                  {"id": "1"},
                  {"id": "2"}
                ]
              }
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(3L, 1L, 2L);
        verify(mockRestTemplate).postForObject(any(String.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("rerank: AI가 일부 문서만 인용한 경우 나머지는 원래 순서로 뒤에 배치")
    void rerank_succeeds_appendsUnrankedDocumentsAfterRankedOnes() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.8),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.7),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.6),
            new Candidate(4L, "제목4", "사용맥락4", "#태그4", 0.5)
        );

        // AI가 2번과 4번만 선택
        String apiResponse = """
            {
              "result": {
                "citedDocuments": [
                  {"id": "2"},
                  {"id": "4"}
                ]
              }
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then - AI가 선택한 2, 4가 먼저 오고, 나머지 1, 3이 원래 순서로 뒤에 옴
        assertThat(result).containsExactly(2L, 4L, 1L, 3L);
    }

    @Test
    @DisplayName("rerank: AI가 문서를 인용하지 않은 경우 fallback 순서 반환")
    void rerank_fallsBack_whenNoDocumentsCited() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.5)
        );

        String apiResponse = """
            {
              "result": {
                "citedDocuments": []
              }
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then - priorScore 순으로 정렬 (0.8, 0.5, 0.3)
        assertThat(result).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("rerank: null 쿼리인 경우 원래 순서 반환")
    void rerank_returnsOriginalOrder_withNullQuery() {
        // given
        String query = null;
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("rerank: 빈 쿼리인 경우 원래 순서 반환")
    void rerank_returnsOriginalOrder_withBlankQuery() {
        // given
        String query = "   ";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("rerank: 빈 후보 리스트인 경우 빈 리스트 반환")
    void rerank_returnsEmptyList_withEmptyCandidates() {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of();

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rerank: API 키가 설정되지 않은 경우 원래 순서 반환")
    void rerank_returnsOriginalOrder_withoutApiKey() {
        // given
        ReflectionTestUtils.setField(sut, "naverApiKey", "");
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("rerank: API 호출 실패 시 fallback 순서 반환")
    void rerank_fallsBack_onApiError() {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.5)
        );

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RestClientException("API 호출 실패"));

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then - priorScore 순으로 정렬 (0.8, 0.5, 0.3)
        assertThat(result).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("rerank: 잘못된 JSON 응답 시 fallback 순서 반환")
    void rerank_fallsBack_onInvalidJsonResponse() {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn("{ invalid json }");

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then - priorScore 순으로 정렬
        assertThat(result).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("rerank: API 호출 시 올바른 요청 본문 전송")
    void rerank_sendsCorrectRequestBody() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        String apiResponse = """
            {
              "result": {
                "citedDocuments": [
                  {"id": "2"},
                  {"id": "1"}
                ]
              }
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        sut.rerank(query, candidates);

        // then
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).postForObject(any(String.class), captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> capturedEntity = captor.getValue();
        Map<String, Object> body = capturedEntity.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("query")).isEqualTo(query);
        assertThat(body.get("documents")).isInstanceOf(List.class);

        List<?> documents = (List<?>) body.get("documents");
        assertThat(documents).hasSize(2);
    }

    @Test
    @DisplayName("rerank: API 호출 시 올바른 헤더 전송")
    void rerank_sendsCorrectHeaders() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3)
        );

        String apiResponse = """
            {
              "result": {
                "citedDocuments": [
                  {"id": "1"}
                ]
              }
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        sut.rerank(query, candidates);

        // then
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).postForObject(any(String.class), captor.capture(), eq(String.class));

        HttpEntity<?> capturedEntity = captor.getValue();
        assertThat(capturedEntity.getHeaders().getContentType()).hasToString("application/json");
        assertThat(capturedEntity.getHeaders().get("Authorization")).containsExactly("Bearer test-api-key");
        assertThat(capturedEntity.getHeaders().get("X-NCP-CLOVASTUDIO-REQUEST-ID")).containsExactly("test-request-id");
    }

    @Test
    @DisplayName("rerank: 후보의 title과 usageContext를 조합하여 문서 생성")
    void rerank_combinesTitleAndUsageContext() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "밈 제목", "사용 맥락", "#태그", 0.8)
        );

        String apiResponse = """
            {
              "result": {
                "citedDocuments": [
                  {"id": "1"}
                ]
              }
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        sut.rerank(query, candidates);

        // then
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).postForObject(any(String.class), captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> capturedEntity = captor.getValue();
        Map<String, Object> body = capturedEntity.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("documents")).isInstanceOf(List.class);

        List<?> documents = (List<?>) body.get("documents");
        assertThat(documents).hasSize(1);

        // Verify the document structure by serializing to JSON and back
        String documentsJson = objectMapper.writeValueAsString(documents.get(0));
        Map<?, ?> firstDocAsMap = objectMapper.readValue(documentsJson, Map.class);

        assertThat(firstDocAsMap.get("id")).isEqualTo("1");
        assertThat(firstDocAsMap.get("doc")).isEqualTo("밈 제목. 사용 맥락");
    }

    @Test
    @DisplayName("rerank: null result 응답 시 fallback 순서 반환")
    void rerank_fallsBack_withNullResult() throws Exception {
        // given
        String query = "재미있는 밈";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        String apiResponse = """
            {
              "result": null
            }
            """;

        when(mockRestTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(apiResponse);

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then - priorScore 순으로 정렬
        assertThat(result).containsExactly(2L, 1L);
    }
}
