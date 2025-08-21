package spring.memewikibe.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.domain.meme.event.MemeViewedEvent;
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
    private final ApplicationEventPublisher eventPublisher;

    public MemeLookUpServiceImpl(CategoryRepository categoryRepository, MemeRepository memeRepository, MemeCategoryRepository memeCategoryRepository, ApplicationEventPublisher eventPublisher) {
        this.categoryRepository = categoryRepository;
        this.memeRepository = memeRepository;
        this.memeCategoryRepository = memeCategoryRepository;
        this.eventPublisher = eventPublisher;
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
        int validatedLimit = Math.min(Math.max(limit, 1), 30);

        Category category = getCategoryBy(id);
        List<Meme> foundMemes = fetchMemesByCategory(category, next, validatedLimit);

        return createPageResponseBy(foundMemes, validatedLimit);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<Cursor, MemeDetailResponse> getMemesByQuery(String query, Long next, int limit) {
        int validatedLimit = Math.min(Math.max(limit, 1), 30);

        if (next == null) {
            List<Meme> foundMemes = memeRepository.findByTitleDynamicContainingOrderByIdDesc(query, Limit.of(validatedLimit + 1));
            return createPageResponseBy(foundMemes, validatedLimit);
        }
        List<Meme> foundMemes = memeRepository.findByTitleDynamicContainingAndIdLessThanOrderByIdDesc(query, next, Limit.of(validatedLimit + 1));
        return createPageResponseBy(foundMemes, validatedLimit);
    }

    @Transactional(readOnly = true)
    @Override
    public MemeDetailResponse getMemeById(Long id) {
        // NORMAL 상태의 밈만 조회 (보안: ABNORMAL 밈은 일반 사용자에게 노출되지 않음)
        Meme meme = memeRepository.findByIdAndNormalFlag(id)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));

        eventPublisher.publishEvent(new MemeViewedEvent(id));

        return MemeDetailResponse.from(meme);
    }

    private PageResponse<Cursor, MemeDetailResponse> createPageResponseBy(List<Meme> memes, int limit) {
        Cursor cursor = Cursor.of(memes, limit);
        List<MemeDetailResponse> response = memes.stream()
            .map(MemeDetailResponse::from)
            .limit(limit)
            .toList();
        return PageResponse.cursor(cursor, response);
    }

    private Category getCategoryBy(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.CATEGORY_NOT_FOUND));
    }

    private List<Meme> fetchMemesByCategory(Category category, Long next, int limit) {
        // NORMAL 상태의 밈만 조회 (보안: ABNORMAL 밈은 일반 사용자에게 노출되지 않음)
        if (next == null) {
            return memeCategoryRepository.findByCategoryAndMemeNormalFlagOrderByMemeIdDesc(category, Limit.of(limit + 1))
                .stream()
                .map(MemeCategory::getMeme)
                .toList();
        }

        return memeCategoryRepository.findByCategoryAndMemeIdLessThanAndMemeNormalFlagOrderByMemeIdDesc(category, next, Limit.of(limit + 1))
            .stream()
            .map(MemeCategory::getMeme)
            .toList();
    }
}
