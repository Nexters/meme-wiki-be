package spring.memewikibe.application;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

@Service
public class MemeLookUpServiceImpl implements MemeLookUpService {

    private final CategoryRepository categoryRepository;
    private final MemeRepository memeRepository;
    private final MemeCategoryRepository memeCategoryRepository;

    public MemeLookUpServiceImpl(CategoryRepository categoryRepository, MemeRepository memeRepository, MemeCategoryRepository memeCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.memeRepository = memeRepository;
        this.memeCategoryRepository = memeCategoryRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
            .stream()
            .map(it -> new CategoryResponse(it.getId(), it.getName(), it.getImgUrl()))
            .toList();
    }
}
