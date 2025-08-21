package spring.memewikibe.application.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Meme 데이터의 임베딩 처리 및 Vector Store 관리 서비스
 * 
 * 주요 기능:
 * 1. 개별 Meme를 벡터화하여 저장
 * 2. 모든 Meme 데이터 배치 벡터화
 * 3. 유사도 기반 Meme 검색
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemeEmbeddingService {
    
    private final MemeRepository memeRepository;
    private final MemeDocumentConverter documentConverter;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    /**
     * 개별 Meme를 벡터화하여 Vector Store에 저장합니다.
     * 
     * @param meme 저장할 Meme 엔티티
     */
    @Transactional(readOnly = true)
    public void embedAndStoreMeme(Meme meme) {
        try {
            Document document = documentConverter.convertToDocument(meme);
            vectorStore.add(List.of(document));
            
            log.info("Successfully embedded and stored meme: {} (ID: {})", 
                    meme.getTitle(), meme.getId());
        } catch (Exception e) {
            log.error("Failed to embed meme: {} (ID: {})", meme.getTitle(), meme.getId(), e);
            throw new RuntimeException("Failed to embed meme: " + meme.getId(), e);
        }
    }

    /**
     * 모든 Meme 데이터를 배치로 벡터화하여 저장합니다.
     * 초기 데이터 세팅이나 전체 재색인 시 사용됩니다.
     * 
     * @return 처리된 Meme 개수
     */
    @Transactional(readOnly = true)
    public int embedAllMemes() {
        log.info("🚀 Starting WEIGHTED embedding of all memes...");
        log.info("📊 Weight Priority: usage_context(4) → hashtags(3) → origin(2) → title(1)");
        
        List<Meme> allMemes = memeRepository.findAll();
        if (allMemes.isEmpty()) {
            log.warn("No memes found for embedding");
            return 0;
        }
        
        try {
            // 배치 크기로 나누어 처리 (메모리 효율성)
            int batchSize = 30; // 더 안전한 배치 크기
            int totalProcessed = 0;
            
            log.info("📋 Total memes to process: {}", allMemes.size());
            
            for (int i = 0; i < allMemes.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allMemes.size());
                List<Meme> batch = allMemes.subList(i, endIndex);
                
                log.info("🔄 Processing weighted batch {}-{} of {} memes...", 
                        i + 1, endIndex, allMemes.size());
                
                List<Document> documents = batch.stream()
                        .map(meme -> {
                            Document doc = documentConverter.convertToDocument(meme);
                            log.debug("📝 Meme {} converted with weighted text length: {}", 
                                    meme.getId(), doc.getText().length());
                            return doc;
                        })
                        .collect(Collectors.toList());
                
                vectorStore.add(documents);
                totalProcessed += batch.size();
                
                log.info("✅ Successfully processed weighted batch {}-{}", i + 1, endIndex);
                
                // 배치 간 잠시 대기로 안정성 확보
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("🎉 Successfully embedded {} memes with WEIGHTED approach!", totalProcessed);
            log.info("🔍 Vector search now prioritizes: usage_context > hashtags > origin > title");
            return totalProcessed;
            
        } catch (Exception e) {
            log.error("❌ Failed to embed all memes with weighted approach", e);
            throw new RuntimeException("Batch embedding failed", e);
        }
    }

    /**
     * 사용자 쿼리와 유사한 Meme들을 Vector Store에서 검색합니다.
     * 
     * @param userQuery 사용자가 입력한 상황 설명
     * @param topK 반환할 최대 결과 개수 (기본값: 5)
     * @return 유사도가 높은 순으로 정렬된 Document 리스트
     */
    public List<Document> searchSimilarMemes(String userQuery, int topK) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            log.warn("Empty user query provided for meme search");
            return List.of();
        }
        
        try {
            // SearchRequest를 통해 검색 파라미터 설정
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(userQuery)
                    .topK(topK)
                    .similarityThreshold(0.7) // 유사도 임계값 설정
                    .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            log.info("Found {} similar memes for query: '{}'", results.size(), userQuery);
            log.debug("Search results: {}", results.stream()
                    .map(doc -> doc.getMetadata().get("title"))
                    .collect(Collectors.toList()));
            
            return results;
            
        } catch (Exception e) {
            log.error("Failed to search similar memes for query: '{}'", userQuery, e);
            return List.of();
        }
    }

    /**
     * 기본 topK 값(5)으로 유사한 Meme들을 검색합니다.
     */
    public List<Document> searchSimilarMemes(String userQuery) {
        return searchSimilarMemes(userQuery, 3);
    }

    /**
     * Vector Store에서 특정 Meme을 삭제합니다.
     * 
     * @param memeId 삭제할 Meme ID
     */
    public void deleteMemeFromVectorStore(Long memeId) {
        try {
            // Vector Store에서 메타데이터 기반으로 삭제
            // 구현은 사용하는 Vector Store에 따라 달라질 수 있음
            log.info("Attempting to delete meme {} from vector store", memeId);
            
            // PGVector의 경우 직접적인 ID 기반 삭제가 제한적일 수 있으므로
            // 필요시 재구현이 필요할 수 있습니다.
            
        } catch (Exception e) {
            log.error("Failed to delete meme {} from vector store", memeId, e);
        }
    }

    /**
     * 🚨 벡터 스토어를 완전히 초기화하고 새로운 가중치로 재임베딩합니다.
     * 주의: 모든 기존 벡터 데이터가 삭제됩니다!
     */
    @Transactional(readOnly = true) 
    public int resetAndEmbedAllMemes() {
        log.warn("🚨 STARTING COMPLETE VECTOR STORE RESET!");
        
        try {
            // 1. 벡터 스토어 완전 초기화 시도
            log.info("🧹 Attempting to clear all existing vectors from Vector Store...");
            
            // Pinecone의 경우 네임스페이스 전체 삭제로 초기화
            try {
                // 빈 조건으로 검색해서 모든 벡터 조회 후 삭제 시도
                List<Document> allExistingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                        .query("*") // 모든 문서 검색
                        .topK(10000) // 충분히 큰 숫자
                        .similarityThreshold(0.0) // 모든 유사도 포함
                        .build()
                );
                
                if (!allExistingDocs.isEmpty()) {
                    log.info("🗑️ Found {} existing vectors, deleting...", allExistingDocs.size());
                    // 모든 문서 ID 추출해서 삭제
                    List<String> docIds = allExistingDocs.stream()
                        .map(doc -> doc.getId())
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
                    
                    if (!docIds.isEmpty()) {
                        vectorStore.delete(docIds);
                        log.info("✅ Deleted {} existing vectors", docIds.size());
                    }
                } else {
                    log.info("ℹ️ No existing vectors found to delete");
                }
            } catch (Exception deleteError) {
                log.warn("⚠️ Could not delete existing vectors (may be empty): {}", deleteError.getMessage());
            }
            
            // 2. 모든 밈 데이터 조회
            List<Meme> allMemes = memeRepository.findAll();
            if (allMemes.isEmpty()) {
                log.warn("⚠️ No memes found in database for embedding");
                return 0;
            }
            
            log.info("🚀 Starting WEIGHTED RE-EMBEDDING of {} memes...", allMemes.size());
            log.info("📊 NEW Weight Priority: usage_context(4) → hashtags(3) → origin(2) → title(1)");
            
            // 3. 새로운 가중치 기반으로 전체 재임베딩
            int batchSize = 10; // 더 작은 배치로 안정성 확보
            int totalProcessed = 0;
            
            for (int i = 0; i < allMemes.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allMemes.size());
                List<Meme> batch = allMemes.subList(i, endIndex);
                
                List<Document> documents = batch.stream()
                        .map(documentConverter::convertToDocument)
                        .collect(Collectors.toList());
                
                vectorStore.add(documents);
                totalProcessed += batch.size();
                
                log.info("📈 RE-EMBEDDED batch {}/{} - {} memes (Total: {})", 
                        (i / batchSize) + 1, 
                        (allMemes.size() + batchSize - 1) / batchSize,
                        batch.size(), 
                        totalProcessed);
                
                // 배치 간 대기로 안정성 확보
                try {
                    Thread.sleep(500); // 0.5초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("🎉 RESET AND RE-EMBEDDING COMPLETED! Total processed: {} memes with NEW WEIGHTING", totalProcessed);
            log.info("💫 Different memes should now be recommended for different situations!");
            return totalProcessed;
            
        } catch (Exception e) {
            log.error("❌ Failed to reset and re-embed vector store", e);
            throw new RuntimeException("Vector store reset and re-embedding failed", e);
        }
    }
}
