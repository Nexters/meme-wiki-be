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

    @GetMapping("/search")
    public ApiResponse<List<MemeSimpleResponse>> search(
        @RequestParam String query,
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        int topK = Math.min(Math.max(limit, 1), 30);
        List<Long> candidateIds = vectorIndexService.query(query, topK);
        // RAG reranking (stubbed)
        String userContext = userId == null ? "" : ("user:" + userId);
        List<Long> ranked = naverRagService.recommendWithContext(userContext, query, candidateIds);

        if (ranked.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<Meme> memes = memeRepository.findAllById(ranked);
        Map<Long, Meme> byId = memes.stream().collect(Collectors.toMap(Meme::getId, Function.identity()));
        List<MemeSimpleResponse> responses = ranked.stream()
            .map(byId::get)
            .filter(m -> m != null)
            .distinct()
            .limit(topK)
            .map(m -> new MemeSimpleResponse(m.getId(), m.getTitle(), m.getImgUrl()))
            .toList();
        return ApiResponse.success(responses);
    }

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
