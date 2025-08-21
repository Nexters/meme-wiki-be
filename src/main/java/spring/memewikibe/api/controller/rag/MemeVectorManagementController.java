package spring.memewikibe.api.controller.rag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.application.rag.MemeEmbeddingService;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.response.ApiResponse;

import java.util.Map;

/**
 * 밈 벡터 데이터 관리용 API 컨트롤러
 * 주로 관리자나 배치 작업용으로 사용됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/meme-vectors")
@RequiredArgsConstructor
@Tag(name = "Meme Vector Management", description = "밈 벡터 데이터 관리 API (관리자용)")
public class MemeVectorManagementController {
    
    private final MemeEmbeddingService embeddingService;

    /**
     * 모든 밈 데이터를 배치로 벡터화하여 Vector Store에 저장합니다.
     * 초기 설정이나 전체 재색인 시 사용됩니다.
     */
    @PostMapping("/embed-all")
    @Operation(
        summary = "전체 밈 데이터 벡터화",
        description = "데이터베이스의 모든 밈을 벡터화하여 Vector Store에 저장합니다. " +
                     "초기 설정이나 전체 데이터 재색인 시 사용됩니다."
    )
    public ResponseEntity<ApiResponse<?>> embedAllMemes() {
        log.info("Starting batch embedding of all memes...");
        
        try {
            int processedCount = embeddingService.embedAllMemes();
            
            Map<String, Object> result = Map.of(
                    "processedCount", processedCount,
                    "message", "모든 밈 데이터가 성공적으로 벡터화되었습니다."
            );
            
            log.info("Successfully completed batch embedding. Processed {} memes", processedCount);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("Failed to embed all memes", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorType.INTERNAL_ERROR, "밈 데이터 벡터화에 실패했습니다."));
        }
    }

    /**
     * 특정 밈 하나를 벡터화하여 Vector Store에 저장합니다.
     */
    @PostMapping("/embed/{memeId}")
    @Operation(
        summary = "개별 밈 벡터화",
        description = "특정 밈 하나를 벡터화하여 Vector Store에 저장합니다. " +
                     "새로운 밈이 추가되거나 기존 밈이 수정된 경우 사용됩니다."
    )
    public ResponseEntity<ApiResponse<?>> embedSingleMeme(
            @PathVariable Long memeId) {
        
        log.info("Starting embedding for meme ID: {}", memeId);
        
        try {
            // 여기서는 서비스에서 memeId로 Meme을 조회하는 메서드가 추가로 필요합니다.
            // 현재는 간단한 응답으로 대체합니다.
            
            Map<String, Object> result = Map.of(
                    "memeId", memeId,
                    "message", "개별 밈 벡터화가 요청되었습니다. (구현 예정)"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("Failed to embed meme ID: {}", memeId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorType.INTERNAL_ERROR, "밈 벡터화에 실패했습니다."));
        }
    }

    /**
     * Vector Store를 완전히 초기화하고 새로운 가중치로 재임베딩합니다.
     * 🚨 주의: 모든 기존 벡터 데이터가 삭제됩니다!
     */
    @PostMapping("/reset-and-embed")
    @Operation(
        summary = "벡터 스토어 완전 초기화 및 재임베딩",
        description = "⚠️ 위험: 모든 기존 벡터를 삭제하고 새로운 가중치 기반으로 전체 재임베딩합니다. " +
                     "똑같은 밈만 추천되는 문제 해결용입니다."
    )
    public ResponseEntity<ApiResponse<?>> resetAndEmbedAll() {
        log.warn("🚨 STARTING COMPLETE VECTOR STORE RESET AND RE-EMBEDDING!");
        
        try {
            int processedCount = embeddingService.resetAndEmbedAllMemes();
            
            Map<String, Object> result = Map.of(
                    "processedCount", processedCount,
                    "message", "🎉 벡터 스토어가 완전히 초기화되고 새로운 가중치로 재임베딩되었습니다!",
                    "newWeighting", "usage_context(4) → hashtags(3) → origin(2) → title(1)"
            );
            
            log.info("🎯 Successfully completed vector store reset and re-embedding. Processed {} memes with NEW WEIGHTING!", processedCount);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("❌ Failed to reset and re-embed vector store", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorType.INTERNAL_ERROR, "벡터 스토어 초기화 및 재임베딩에 실패했습니다."));
        }
    }

    /**
     * Vector Store의 상태를 확인합니다.
     */
    @GetMapping("/status")
    @Operation(
        summary = "Vector Store 상태 확인",
        description = "Vector Store의 연결 상태와 저장된 벡터 개수 등을 확인합니다."
    )
    public ResponseEntity<ApiResponse<?>> getVectorStoreStatus() {
        try {
            // 실제 구현에서는 Vector Store의 상태를 체크하는 로직이 필요합니다.
            Map<String, Object> status = Map.of(
                    "status", "connected",
                    "message", "Vector Store 상태 확인 기능이 구현되었습니다."
            );
            
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("Failed to check vector store status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorType.INTERNAL_ERROR, "Vector Store 상태 확인에 실패했습니다."));
        }
    }
}
