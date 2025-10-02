package spring.memewikibe.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class NaverCrossEncoderReranker implements CrossEncoderReranker {

    @Value("${NAVER_AI_API_KEY:}")
    private String naverApiKey;
    @Value("${NAVER_AI_REQUEST_ID:meme-wiki-reranker}")
    private String naverRequestId;
    @Value("${NAVER_AI_RERANKER_ENDPOINT:https://clovastudio.stream.ntruss.com/v1/api-tools/reranker}")
    private String rerankerApiEndpoint;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // --- [핵심 변경] RAG API 응답 구조에 맞는 DTO(Record) 정의 ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApiResponse(ApiResult result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApiResult(List<CitedDocument> citedDocuments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CitedDocument(String id) {} // 'doc' 필드는 무시

    // API 요청 DTO
    private record RerankDocument(String id, String doc) {}
    // -------------------------------------------------------------

    @Override
    public List<Long> rerank(String query, List<CrossEncoderReranker.Candidate> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return candidates.stream().map(CrossEncoderReranker.Candidate::id).toList();
        }
        if (naverApiKey == null || naverApiKey.isBlank()) {
            log.warn("NAVER_AI_API_KEY is not set. Skipping CrossEncoder reranking.");
            return candidates.stream().map(CrossEncoderReranker.Candidate::id).toList();
        }

        try {
            List<RerankDocument> documents = candidates.stream()
                .map(c -> new RerankDocument(String.valueOf(c.id()), c.title() + ". " + c.usageContext()))
                .toList();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + naverApiKey);
            headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", naverRequestId);

            Map<String, Object> requestBody = Map.of(
                "query", query,
                "documents", documents
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            String responseStr = restTemplate.postForObject(rerankerApiEndpoint, requestEntity, String.class);
            log.debug("Reranker (RAG) Raw Response: {}", responseStr);

            // --- [핵심 변경] RAG 응답에서 'citedDocuments'를 추출하여 재정렬 로직 수행 ---

            ApiResponse response = objectMapper.readValue(responseStr, ApiResponse.class);

            if (response == null || response.result() == null || response.result().citedDocuments() == null || response.result().citedDocuments().isEmpty()) {
                log.info("Reranker did not cite any documents. Falling back to initial order.");
                return fallbackOrder(candidates);
            }

            // 1. AI가 선택한 (인용한) 문서 ID 목록을 순서대로 추출
            List<Long> rankedIds = response.result().citedDocuments().stream()
                .map(doc -> Long.parseLong(doc.id()))
                .toList();

            Set<Long> rankedIdSet = Set.copyOf(rankedIds);

            // 2. 최종 순서 리스트를 생성: AI가 선택한 ID를 먼저 넣고,
            //    선택되지 않은 나머지 ID들을 원래 순서(또는 초기 점수 순)대로 뒤에 붙임
            List<Long> finalOrder = new ArrayList<>(rankedIds);

            candidates.stream()
                .map(CrossEncoderReranker.Candidate::id)
                .filter(id -> !rankedIdSet.contains(id))
                .forEach(finalOrder::add);

            log.info("Reranking successful. New order starts with: {}", finalOrder.stream().limit(5).toList());
            return finalOrder;
            // -----------------------------------------------------------------------------

        } catch (Exception e) {
            log.error("Failed to rerank with CrossEncoder. Falling back to initial order.", e);
            return fallbackOrder(candidates);
        }
    }

    private List<Long> fallbackOrder(List<CrossEncoderReranker.Candidate> candidates) {
        return candidates.stream()
            .sorted(Comparator.comparingDouble(CrossEncoderReranker.Candidate::priorScore).reversed())
            .map(CrossEncoderReranker.Candidate::id)
            .toList();
    }
}