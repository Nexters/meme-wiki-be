package spring.memewikibe.api.controller.meme;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.application.MemeAggregationLookUpCacheProxyService;
import spring.memewikibe.application.MemeAggregationLookUpService;
import spring.memewikibe.application.MemeAggregationService;
import spring.memewikibe.application.MemeCreateService;
import spring.memewikibe.application.MemeLookUpService;
import spring.memewikibe.support.response.ApiResponse;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/memes")
public class MemeController {

    private final MemeAggregationService aggregationService;
    private final MemeLookUpService memeLookUpService;
    private final MemeAggregationLookUpService memeAggregationLookUpService;
    private final MemeCreateService memeCreateService;

    public MemeController(MemeAggregationService aggregationService, MemeLookUpService memeLookUpService, MemeAggregationLookUpCacheProxyService memeAggregationLookUpService, MemeCreateService memeCreateService) {
        this.aggregationService = aggregationService;
        this.memeLookUpService = memeLookUpService;
        this.memeAggregationLookUpService = memeAggregationLookUpService;
        this.memeCreateService = memeCreateService;
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
    public ApiResponse<List<MemeSimpleResponse>> getMostSharedMemes() {
        return ApiResponse.success(memeAggregationLookUpService.getMostFrequentSharedMemes());
    }

    @GetMapping("/rankings/custom")
    public ApiResponse<List<MemeSimpleResponse>> getMostCustomMemes() {
        return ApiResponse.success(memeAggregationLookUpService.getMostFrequentCustomMemes());
    }

    @GetMapping("/rankings/top-rated")
    public ApiResponse<List<MemeSimpleResponse>> getTopRatedMemes() {
        return ApiResponse.success(memeAggregationLookUpService.getMostPopularMemes());
    }

    @Operation(
        summary = "밈 생성",
        requestBody = @RequestBody(
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                encoding = {
                    @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE),
                    @Encoding(name = "image", contentType = "image/*")
                }
            )
        )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> createMeme(
        @RequestPart("request") @Valid MemeCreateRequest request,
        @RequestPart("image") MultipartFile imageFile) {
        return ApiResponse.success(memeCreateService.createMeme(request, imageFile));
    }
}
