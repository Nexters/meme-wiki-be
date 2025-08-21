package spring.memewikibe.api.controller.rag.response;

import lombok.Builder;
import lombok.Getter;
import spring.memewikibe.application.rag.MemeRecommendationResult;

/**
 * 밈 추천 응답 DTO
 */
@Getter
@Builder
public class MemeRecommendationResponse {
    
    /**
     * 사용자가 입력한 상황
     */
    private final String userSituation;
    
    /**
     * 추천된 밈 정보
     */
    private final RecommendedMeme recommendedMeme;
    
    /**
     * LLM이 생성한 추천 이유 및 설명
     */
    private final String explanation;
    
    /**
     * 추천 메타 정보
     */
    private final RecommendationMetadata metadata;
    
    /**
     * 도메인 결과로부터 응답 DTO를 생성합니다.
     */
    public static MemeRecommendationResponse from(MemeRecommendationResult result) {
        RecommendedMeme recommendedMeme = null;
        
        if (result.hasRecommendation()) {
            recommendedMeme = RecommendedMeme.builder()
                    .id(result.getRecommendedMemeId())
                    .title(result.getRecommendedMemeTitle())
                    .imageUrl(result.getRecommendedMemeImageUrl())
                    .build();
        }
        
        RecommendationMetadata metadata = RecommendationMetadata.builder()
                .similarMemesFound(result.getSimilarMemesCount())
                .hasRecommendation(result.hasRecommendation())
                .build();
        
        return MemeRecommendationResponse.builder()
                .userSituation(result.getUserSituation())
                .recommendedMeme(recommendedMeme)
                .explanation(result.getExplanation())
                .metadata(metadata)
                .build();
    }
    
    /**
     * 추천된 밈 정보
     */
    @Getter
    @Builder
    public static class RecommendedMeme {
        private final Long id;
        private final String title;
        private final String imageUrl;
    }
    
    /**
     * 추천 메타데이터
     */
    @Getter
    @Builder
    public static class RecommendationMetadata {
        /**
         * Vector Store에서 찾은 유사한 밈의 개수
         */
        private final Integer similarMemesFound;
        
        /**
         * 추천 결과 존재 여부
         */
        private final Boolean hasRecommendation;
    }
}
