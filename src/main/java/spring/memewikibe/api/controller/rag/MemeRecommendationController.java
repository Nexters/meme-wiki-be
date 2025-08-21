package spring.memewikibe.api.controller.rag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.rag.request.MemeRecommendationRequest;
import spring.memewikibe.api.controller.rag.response.MemeRecommendationResponse;
import spring.memewikibe.application.rag.MemeRecommendationResult;
import spring.memewikibe.application.rag.MemeRecommendationService;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.response.ApiResponse;

/**
 * RAG 기반 상황별 밈 추천 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/meme-recommendations")
@RequiredArgsConstructor
@Tag(name = "Meme Recommendation", description = "RAG 기반 상황별 밈 추천 API")
public class MemeRecommendationController {
    
    private final MemeRecommendationService recommendationService;

    /**
     * 사용자의 상황에 맞는 밈을 추천합니다.
     * 
     * @param request 사용자 상황 설명이 포함된 요청
     * @return RAG 기반으로 추천된 밈과 설명
     */
    @PostMapping
    @Operation(
        summary = "상황별 밈 추천",
        description = "사용자가 입력한 상황에 가장 적합한 밈을 AI가 추천해줍니다. " +
                     "Vector Store에서 유사한 밈들을 검색하고, LLM이 최적의 밈을 선택하여 이유와 함께 설명합니다."
    )
    public ResponseEntity<ApiResponse<?>> recommendMeme(
            @Valid @RequestBody MemeRecommendationRequest request) {
        
        log.info("Received meme recommendation request for situation: '{}'", request.getSituation());
        
        try {
            MemeRecommendationResult result = recommendationService.recommendMeme(request.getSituation());
            MemeRecommendationResponse response = MemeRecommendationResponse.from(result);
            
            log.info("Successfully processed meme recommendation. Has recommendation: {}", 
                    result.hasRecommendation());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for meme recommendation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorType.INVALID_REQUEST, e.getMessage()));
            
        } catch (Exception e) {
            log.error("Unexpected error during meme recommendation", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorType.INTERNAL_ERROR, "밈 추천 중 오류가 발생했습니다."));
        }
    }
}
