package spring.memewikibe.api.controller.meme;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.api.controller.meme.response.MostSharedMemes;
import spring.memewikibe.application.*;
import spring.memewikibe.support.response.ApiResponse;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

/**
 * 밈 조회 및 상호작용 API
 * 
 * 📝 밈 생성 기능은 관리자 페이지에서만 가능합니다.
 * - 관리자 로그인: GET /admin/login
 * - 밈 생성: POST /admin/memes (관리자 인증 필요)
 */
@RestController
@RequestMapping("/api/memes")
public class MemeController {

    private final MemeAggregationService aggregationService;
    private final MemeLookUpService memeLookUpService;
    private final MemeAggregationLookUpService memeAggregationLookUpService;
    private final SharedMemeScheduleCacheService sharedMemeScheduleCacheService;

    public MemeController(MemeAggregationService aggregationService, MemeLookUpService memeLookUpService, MemeAggregationLookUpCacheProxyService memeAggregationLookUpService, SharedMemeScheduleCacheService sharedMemeScheduleCacheService) {
        this.aggregationService = aggregationService;
        this.memeLookUpService = memeLookUpService;
        this.memeAggregationLookUpService = memeAggregationLookUpService;
        this.sharedMemeScheduleCacheService = sharedMemeScheduleCacheService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Cursor, MemeDetailResponse>> getMemes(
        @RequestParam(required = false) Long next,
        @RequestParam(required = false) String query,
        @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        return ApiResponse.success(memeLookUpService.getMemesByQuery(query, next, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemeDetailResponse> getMeme(
        @PathVariable Long id
    ) {
        return ApiResponse.success(memeLookUpService.getMemeById(id));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{id}/custom")
    public void makeOwnMeme(
        @PathVariable Long id
    ) {
        aggregationService.increaseMakeCustomMemeCount(id);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{id}/share")
    public void shareMeme(
        @PathVariable Long id
    ) {
        aggregationService.increaseShareMemeCount(id);
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(memeLookUpService.getAllCategories());
    }

    @GetMapping("/categories/{id}")
    public ApiResponse<PageResponse<Cursor, MemeDetailResponse>> getMemesByCategory(
        @PathVariable Long id,
        @RequestParam(required = false) Long next,
        @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        return ApiResponse.success(memeLookUpService.getMemesByCategory(id, next, limit));
    }

    @GetMapping("/rankings/shared")
    public ApiResponse<MostSharedMemes> getMostSharedMemes() {
        return ApiResponse.success(sharedMemeScheduleCacheService.getMostSharedMemes());
    }

    @GetMapping("/rankings/custom")
    public ApiResponse<List<MemeSimpleResponse>> getMostCustomMemes() {
        return ApiResponse.success(memeAggregationLookUpService.getMostFrequentCustomMemes());
    }

    @GetMapping("/rankings/top-rated")
    public ApiResponse<List<MemeSimpleResponse>> getTopRatedMemes() {
        return ApiResponse.success(memeAggregationLookUpService.getMostPopularMemes());
    }

    // 밈 생성 기능은 관리자 페이지에서만 가능합니다.
    // POST /admin/memes 를 사용하세요.
}
