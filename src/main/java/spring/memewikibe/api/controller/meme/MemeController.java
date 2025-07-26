package spring.memewikibe.api.controller.meme;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.support.response.ApiResponse;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

@RestController
@RequestMapping("/api/memes")
public class MemeController {

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
        // TODO: 증적용 테이블에 나만의 밈 만들기 로그 증적
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{id}/share")
    public void shareMeme(
        @PathVariable Long id
    ) {
        // TODO: 증적용 테이블에 공유 로그 증적
    }

}
