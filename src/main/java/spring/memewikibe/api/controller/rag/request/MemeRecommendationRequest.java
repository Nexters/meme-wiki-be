package spring.memewikibe.api.controller.rag.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 밈 추천 요청 DTO
 */
@Getter
@NoArgsConstructor
public class MemeRecommendationRequest {
    
    /**
     * 사용자의 상황 설명
     * 예: "나 오늘 시험 망했어. 진짜 허무하다."
     */
    @NotBlank(message = "상황 설명을 입력해주세요.")
    @Size(min = 5, max = 500, message = "상황 설명은 5자 이상 500자 이하로 입력해주세요.")
    private String situation;
    
    public MemeRecommendationRequest(String situation) {
        this.situation = situation;
    }
}
