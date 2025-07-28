package spring.memewikibe.api.controller.meme;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.application.MemeAggregationService;
import spring.memewikibe.application.MemeLookUpService;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.support.response.ApiResponse;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/memes")
public class MemeController {

    private final MemeAggregationService aggregationService;
    private final MemeLookUpService memeLookUpService;

    public MemeController(MemeAggregationService aggregationService, MemeLookUpService memeLookUpService) {
        this.aggregationService = aggregationService;
        this.memeLookUpService = memeLookUpService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Cursor, MemeSimpleResponse>> getMemes(
        @RequestParam(required = false) Long next,
        @RequestParam(required = false) String query,
        @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        // TODO: 검색 및 전체조회
        return null;
    }

    @GetMapping("/{id}")
    public ApiResponse<MemeDetailResponse> getMeme(
        @PathVariable Long id
    ) {
        // TODO: 상세 조회
        return null;
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{id}/own")
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
}
