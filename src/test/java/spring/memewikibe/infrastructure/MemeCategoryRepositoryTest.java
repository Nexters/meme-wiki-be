package spring.memewikibe.infrastructure;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;

import java.util.List;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeCategoryRepositoryTest {

    private final MemeCategoryRepository sut;
    private final MemeRepository memeRepository;
    private final CategoryRepository categoryRepository;

    MemeCategoryRepositoryTest(MemeCategoryRepository sut, MemeRepository memeRepository, CategoryRepository categoryRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * 테스트용 카테고리 생성 헬퍼 메서드
     */
    private Category createCategory(String name, String imgUrl) {
        return Category.builder()
            .name(name)
            .imgUrl(imgUrl)
            .build();
    }

    /**
     * 테스트용 밈 생성 헬퍼 메서드 (전체 필드)
     */
    private Meme createMeme(String title, String origin, String usageContext, String trendPeriod, String hashtags, Meme.Flag flag) {
        return Meme.builder()
            .title(title)
            .origin(origin)
            .usageContext(usageContext)
            .trendPeriod(trendPeriod)
            .hashtags(hashtags)
            .flag(flag)
            .build();
    }

    /**
     * 테스트용 밈 생성 헬퍼 메서드 (최소 필드)
     */
    private Meme createMinimalMeme(String title, Meme.Flag flag) {
        return Meme.builder()
            .title(title)
            .origin("테스트 출처")
            .usageContext("테스트 사용맥락")
            .trendPeriod("2024")
            .hashtags("[\"#테스트\"]")
            .flag(flag)
            .build();
    }

    @Test
    void findByCategoryTest() {
        // given
        Category 예능 = createCategory("예능", "https://example.com/entertainment.jpg");
        Category 연예인 = createCategory("연예인", "https://example.com/celebrity.jpg");
        categoryRepository.saveAll(List.of(예능, 연예인));

        Meme 나만_아니면_돼 = createMinimalMeme("나만 아니면 돼", Meme.Flag.NORMAL);
        Meme 원영적_사고 = createMinimalMeme("원영적 사고", Meme.Flag.NORMAL);
        Meme 무야호 = createMinimalMeme("무야호", Meme.Flag.NORMAL);

        memeRepository.saveAll(List.of(나만_아니면_돼, 원영적_사고, 무야호));
        sut.saveAll(List.of(
            MemeCategory.builder()
                .category(예능)
                .meme(나만_아니면_돼)
                .build(),
            MemeCategory.builder()
                .category(연예인)
                .meme(원영적_사고)
                .build(),
            MemeCategory.builder()
                .category(예능)
                .meme(무야호)
                .build()));

        // when
        List<MemeCategory> memeCategories = sut.findByCategoryAndMemeNormalFlagOrderByMemeIdDesc(예능, Limit.of(1));

        // then
        BDDAssertions.then(memeCategories).hasSize(1);
    }

    @Description("cursor pagination에서 DESC 정렬 시 현재 ID보다 작은 밈들을 조회")
    @Test
    void findByCategoryAndMemeIdLessThanOrderByMemeIdDescTest() {
        // given
        Category 예능 = createCategory("예능", "https://example.com/entertainment.jpg");
        Category 연예인 = createCategory("연예인", "https://example.com/celebrity.jpg");
        categoryRepository.saveAll(List.of(예능, 연예인));

        Meme 나만_아니면_돼 = createMinimalMeme("나만 아니면 돼", Meme.Flag.NORMAL);
        Meme 원영적_사고 = createMinimalMeme("원영적 사고", Meme.Flag.NORMAL);
        Meme 무야호 = createMinimalMeme("무야호", Meme.Flag.NORMAL);

        memeRepository.saveAll(List.of(나만_아니면_돼, 원영적_사고, 무야호));
        sut.saveAll(List.of(
            MemeCategory.builder()
                .category(예능)
                .meme(나만_아니면_돼)
                .build(),
            MemeCategory.builder()
                .category(연예인)
                .meme(원영적_사고)
                .build(),
            MemeCategory.builder()
                .category(예능)
                .meme(무야호)
                .build()));

        // when
        List<MemeCategory> memeCategories = sut.findByCategoryAndMemeIdLessThanAndMemeNormalFlagOrderByMemeIdDesc(예능, 무야호.getId(), Limit.of(1));

        // then
        BDDAssertions.then(memeCategories).hasSize(1);
    }
    
    @Test
    void NORMAL_Flag_밈만_조회한다() {
        // given
        Category 예능 = createCategory("예능", "https://example.com/entertainment.jpg");
        categoryRepository.save(예능);

        Meme normal밈 = createMeme("정상 밈", "정상 밈의 출처", "정상 밈의 사용맥락", "2024", "[\"#정상\"]", Meme.Flag.NORMAL);
        Meme abnormal밈 = createMeme("비정상 밈", "비정상 밈의 출처", "비정상 밈의 사용맥락", "2024", "[\"#비정상\"]", Meme.Flag.ABNORMAL);

        memeRepository.saveAll(List.of(normal밈, abnormal밈));
        sut.saveAll(List.of(
            MemeCategory.builder().category(예능).meme(normal밈).build(),
            MemeCategory.builder().category(예능).meme(abnormal밈).build()
        ));

        // when: NORMAL Flag 밈만 조회
        List<MemeCategory> normalMemeCategories = sut.findByCategoryAndMemeNormalFlagOrderByMemeIdDesc(예능, Limit.of(10));

        // then: NORMAL 밈만 조회됨
        BDDAssertions.then(normalMemeCategories).hasSize(1);
        BDDAssertions.then(normalMemeCategories.get(0).getMeme().getTitle()).isEqualTo("정상 밈");
        BDDAssertions.then(normalMemeCategories.get(0).getMeme().getFlag()).isEqualTo(Meme.Flag.NORMAL);
    }
    
    @Test
    void NORMAL_Flag_밈만_페이지네이션으로_조회한다() {
        // given
        Category 예능 = createCategory("예능", "https://example.com/entertainment.jpg");
        categoryRepository.save(예능);

        // ID 순서가 보장되도록 순차적으로 저장
        Meme normal밈1 = memeRepository.save(createMinimalMeme("정상 밈 1", Meme.Flag.NORMAL));
        Meme abnormal밈 = memeRepository.save(createMinimalMeme("비정상 밈", Meme.Flag.ABNORMAL));
        Meme normal밈2 = memeRepository.save(createMinimalMeme("정상 밈 2", Meme.Flag.NORMAL));

        sut.saveAll(List.of(
            MemeCategory.builder().category(예능).meme(normal밈1).build(),
            MemeCategory.builder().category(예능).meme(abnormal밈).build(),
            MemeCategory.builder().category(예능).meme(normal밈2).build()
        ));

        // when: lastMemeId보다 작은 NORMAL Flag 밈만 조회
        List<MemeCategory> normalMemeCategories = sut.findByCategoryAndMemeIdLessThanAndMemeNormalFlagOrderByMemeIdDesc(
            예능, normal밈2.getId(), Limit.of(10));

        // then: normal밈1만 조회됨 (abnormal밈은 제외)
        BDDAssertions.then(normalMemeCategories).hasSize(1);
        BDDAssertions.then(normalMemeCategories.get(0).getMeme().getTitle()).isEqualTo("정상 밈 1");
        BDDAssertions.then(normalMemeCategories.get(0).getMeme().getFlag()).isEqualTo(Meme.Flag.NORMAL);
    }
}