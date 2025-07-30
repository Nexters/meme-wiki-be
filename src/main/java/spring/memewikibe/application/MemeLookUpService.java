package spring.memewikibe.application;

import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

public interface MemeLookUpService {
    List<CategoryResponse> getAllCategories();

    PageResponse<Cursor, MemeDetailResponse> getMemesByCategory(Long id, Long next, int limit);

    PageResponse<Cursor, MemeDetailResponse> getMemesByQuery(String query, Long next, int limit);
}
