package spring.memewikibe.api.controller.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.memewikibe.api.controller.recommendation.response.MemeRecommendationResponse;
import spring.memewikibe.application.RecommendationService;
import spring.memewikibe.support.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;


    @GetMapping("/search-explain")
    public ApiResponse<List<MemeRecommendationResponse>> searchExplain(
        @RequestParam String query,
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "3") int limit
    ) {
        List<MemeRecommendationResponse> out = recommendationService.searchWithReasons(query, userId, limit);
        return ApiResponse.success(out);
    }
}
