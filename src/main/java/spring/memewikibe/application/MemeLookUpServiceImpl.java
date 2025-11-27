package spring.memewikibe.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
        int validatedLimit = validateLimit(limit);

        Category category = getCategoryBy(id);
        Slice<MemeCategory> slice = memeCategoryRepository.findNormalMemesWithCursorAsSlice(
            category,
            next,
            PageRequest.of(0, validatedLimit)
        );

        Cursor cursor = Cursor.fromSlice(slice, mc -> mc.getMeme().getId());
        List<MemeDetailResponse> response = slice.getContent().stream()
            .map(MemeCategory::getMeme)
            .map(MemeDetailResponse::from)
            .toList();

        return PageResponse.cursor(cursor, response);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<Cursor, MemeDetailResponse> getMemesByQuery(String query, Long next, int limit) {
        int validatedLimit = validateLimit(limit);

        Slice<Meme> slice = memeRepository.findByTitleOrHashtagsContainingAsSlice(
            query,
            next,
            PageRequest.of(0, validatedLimit)
        );

        Cursor cursor = Cursor.fromSlice(slice);
        List<MemeDetailResponse> response = slice.getContent().stream()
            .map(MemeDetailResponse::from)
            .toList();

        return PageResponse.cursor(cursor, response);
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

    @Transactional(readOnly = true)
    @Override
    public List<Meme> getOrderedMemesByIds(List<Long> ids) {
        List<Meme> memes = memeRepository.findByIdIn(ids);

        var memeMap = memes.stream()
            .collect(Collectors.toMap(Meme::getId, meme -> meme));

        return ids.stream()
            .map(memeMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private Category getCategoryBy(Long id) {
        if (id == null || id <= 0L) {
            return null;
        }
        return categoryRepository.findById(id)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.CATEGORY_NOT_FOUND));
    }


    private int validateLimit(int limit) {
        return Math.min(Math.max(limit, 1), 30);
    }
}
