package spring.memewikibe.application;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.*;

@Transactional
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeLookUpServiceImplTest {

    private final MemeLookUpServiceImpl memeLookUpService;
    private final CategoryRepository categoryRepository;

    MemeLookUpServiceImplTest(MemeLookUpServiceImpl memeLookUpService, CategoryRepository categoryRepository) {
        this.memeLookUpService = memeLookUpService;
        this.categoryRepository = categoryRepository;
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAllInBatch();
    }

    @Test
    void 밈_카테고리를_조회한다() {
        // given
        categoryRepository.saveAll(List.of(
            Category.builder()
            .name("영화")
                .imgUrl("https://example.com/movie.jpg")
            .build(),
            Category.builder()
                .name("드라마")
                .imgUrl("https://example.com/drama.jpg")
                .build()));
        // when
        List<CategoryResponse> responses = memeLookUpService.getAllCategories();
        // then
        then(responses).hasSize(2)
            .extracting(CategoryResponse::name)
            .containsExactlyInAnyOrder("영화", "드라마");
    }

}