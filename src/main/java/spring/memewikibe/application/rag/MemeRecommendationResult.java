package spring.memewikibe.application.rag;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG 기반 밈 추천 서비스의 결과를 담는 DTO
 */
@Getter
@Builder
public class MemeRecommendationResult {
    
    /**
     * 사용자가 입력한 상황 설명
     */
    private final String userSituation;
    
    /**
     * 추천된 밈의 ID (추천 결과가 없으면 null)
     */
    private final Long recommendedMemeId;
    
    /**
     * 추천된 밈의 제목
     */
    private final String recommendedMemeTitle;
    
    /**
     * 추천된 밈의 이미지 URL
     */
    private final String recommendedMemeImageUrl;
    
    /**
     * LLM이 생성한 추천 이유 및 설명
     */
    private final String explanation;
    
    /**
     * Vector Store에서 검색된 유사한 밈의 개수
     */
    private final Integer similarMemesCount;
    
    /**
     * 추천 결과가 있는지 확인
     */
    public boolean hasRecommendation() {
        return recommendedMemeId != null;
    }
}
