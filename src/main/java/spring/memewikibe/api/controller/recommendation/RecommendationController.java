package spring.memewikibe.api.controller.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.api.controller.recommendation.response.MemeRecommendationResponse;
import spring.memewikibe.application.RecommendationService;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;
import spring.memewikibe.infrastructure.ai.NaverRagService;
import spring.memewikibe.support.response.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final MemeVectorIndexService vectorIndexService;
    private final NaverRagService naverRagService;
    private final MemeRepository memeRepository;
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
