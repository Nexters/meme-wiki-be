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

    @Transactional(readOnly = true)
    @Override
    public PageResponse<Cursor, MemeDetailResponse> getMemesByCategory(Long id, Long next, int limit) {
        Category category = getCategoryBy(id);
        List<Meme> foundMemes = fetchMemesByCategory(category, next, limit);

        return createPageResponseBy(foundMemes, limit);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<Cursor, MemeDetailResponse> getMemesByQuery(String query, Long next, int limit) {
        if (next == null) {
            List<Meme> foundMemes = memeRepository.findByTitleContainingOrderByIdDesc(query, Limit.of(limit + 1));
            return createPageResponseBy(foundMemes, limit);
        }
        List<Meme> foundMemes = memeRepository.findByTitleContainingAndIdLessThanOrderByIdDesc(query, next, Limit.of(limit + 1));
        return createPageResponseBy(foundMemes, limit);
    }

    @Transactional(readOnly = true)
    @Override
    public MemeDetailResponse getMemeById(Long id) {
        Meme meme = memeRepository.findById(id)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));
        return new MemeDetailResponse(meme.getId(), meme.getTitle(), meme.getUsageContext(), meme.getOrigin(), meme.getTrendPeriod(), meme.getImgUrl(), meme.getHashtags());
    }

    private PageResponse<Cursor, MemeDetailResponse> createPageResponseBy(List<Meme> memes, int limit) {
        Cursor cursor = Cursor.of(memes, limit);
        List<MemeDetailResponse> response = memes.stream()
            .map(it -> new MemeDetailResponse(it.getId(), it.getTitle(), it.getUsageContext(), it.getOrigin(), it.getTrendPeriod(), it.getImgUrl(), it.getHashtags()))
            .limit(limit)
            .toList();
        return PageResponse.cursor(cursor, response);
    }

    private Category getCategoryBy(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.CATEGORY_NOT_FOUND));
    }

    private List<Meme> fetchMemesByCategory(Category category, Long next, int limit) {
        if (next == null) {
            return memeCategoryRepository.findByCategory(category, Limit.of(limit + 1))
                .stream()
                .map(MemeCategory::getMeme)
                .toList();
        }

        Meme meme = memeRepository.findById(next)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));
        return memeCategoryRepository.findByCategoryAndMemeGreaterThanOrderByMemeAsc(category, meme, Limit.of(limit + 1))
            .stream()
            .map(MemeCategory::getMeme)
            .toList();
    }
}
