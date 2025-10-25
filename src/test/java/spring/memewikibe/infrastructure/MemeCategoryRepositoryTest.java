package spring.memewikibe.infrastructure;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import spring.memewikibe.api.controller.meme.response.MemeDetailResponse;
import spring.memewikibe.application.MemeLookUpService;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeCategoryRepositoryTest {

    private final MemeCategoryRepository sut;
    private final MemeRepository memeRepository;
    private final CategoryRepository categoryRepository;
    private final MemeLookUpService memeLookUpService;

    MemeCategoryRepositoryTest(MemeCategoryRepository sut, MemeRepository memeRepository, CategoryRepository categoryRepository, MemeLookUpService memeLookUpService) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.categoryRepository = categoryRepository;
        this.memeLookUpService = memeLookUpService;
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
        List<MemeCategory> memeCategories = sut.findNormalMemesWithCursor(예능, null, Limit.of(1));

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
        List<MemeCategory> memeCategories = sut.findNormalMemesWithCursor(예능, 무야호.getId(), Limit.of(1));

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
        List<MemeCategory> normalMemeCategories = sut.findNormalMemesWithCursor(예능, null, Limit.of(10));

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
        List<MemeCategory> normalMemeCategories = sut.findNormalMemesWithCursor(
            예능, normal밈2.getId(), Limit.of(10));

        // then: normal밈1만 조회됨 (abnormal밈은 제외)
        BDDAssertions.then(normalMemeCategories).hasSize(1);
        BDDAssertions.then(normalMemeCategories.get(0).getMeme().getTitle()).isEqualTo("정상 밈 1");
        BDDAssertions.then(normalMemeCategories.get(0).getMeme().getFlag()).isEqualTo(Meme.Flag.NORMAL);
    }

    @Test
    void 카테고리_전체보기_조회() {
        Category 예능 = Category.builder()
            .name("예능")
            .imgUrl("https://example.com/entertainment.jpg")
            .build();
        Category 연예인 = Category.builder()
            .name("연예인")
            .imgUrl("https://example.com/celebrity.jpg")
            .build();

        categoryRepository.saveAll(List.of(예능, 연예인));

        Meme 나만_아니면_돼 = Meme.builder()
            .title("나만 아니면 돼")
            .origin("KBS 2TV '1박 2일'의 복불복 게임에서 벌칙에 걸리지 않은 멤버들이 외치던 말에서 유래했습니다. 초창기 멤버 노홍철이 처음 사용했으며, 이후 강호동이 과장된 표정과 액션으로 외치며 짤방으로 널리 퍼졌습니다. 무한도전의 '무한이기주의'와 비교되며 승리자의 자축을 넘어 타인의 불행을 조롱하는 의미로 변모했습니다.")
            .usageContext("원래는 복불복에서 살아남은 자의 자축이었으나, 시간이 지나며 타인의 불행을 보고 자신은 괜찮다며 조롱하는 용법으로 사용됩니다. 인터넷 커뮤니티에서 고소 사건, 단체 손해, 가해자 특정, 지탄받는 대상의 천벌 등 남의 불행에 연루되지 않은 사람들이 이를 비웃을 때 쓰입니다. 2020년대 이후 '알빠노' 등 극단적 이기주의 밈의 시초격으로 인식됩니다.")
            .trendPeriod("2020")
            .hashtags("[\"#1박2일\", \"#복불복\", \"#노홍철\", \"#강호동\", \"#이기주의\", \"#남의불행\", \"#짤방\", \"#인터넷밈\", \"#알빠노\"]")
            .flag(Meme.Flag.NORMAL)
            .build();
        Meme 원영적_사고 = Meme.builder()
            .title("원영적 사고")
            .origin("장원영의 평소 초긍정적 사고방식이 팬들 사이에서 알려져 있었으나, 2024년 3월 15일 팬 계정의 프메 패러디 트윗이 SNS에 퍼지며 대중화되었습니다. 이후 기업 세미나에 등장하며 확산되었고, 그녀의 긍정적 태도가 악플 속에서도 빛나며 큰 공감을 얻었습니다. '럭키비키'는 핵심 문구입니다.")
            .usageContext("자신에게 일어나는 모든 일이 결국 긍정적인 결과로 이어질 것이라는 초월적인 낙관주의를 표현할 때 사용됩니다. 부정적인 상황을 단순히 외면하는 것이 아니라, 현재의 어려움도 결국 자신을 성장시키는 과정으로 받아들이며 긍정적으로 치환하는 사고방식입니다. \"\"완전 럭키비키잖아~\"\"와 함께 쓰입니다.")
            .trendPeriod("2024")
            .hashtags("[\"#장원영\", \"#원영적사고\", \"#럭키비키\", \"#초긍정\", \"#긍정적사고\", \"#인터넷밈\", \"#아이브\"]")
            .flag(Meme.Flag.NORMAL)
            .build();
        Meme 무야호 = Meme.builder()
            .title("무야호")
            .origin("2010년 MBC 예능 프로그램 '무한도전' 알래스카 특집에서 유재석, 정형돈, 노홍철이 현지 교민 최규재 할아버지에게 '무한~도전!' 구호를 요청하자, 할아버지가 당황하며 '무야~ 호~!'라고 외친 장면에서 유래했습니다. 방영 당시에는 큰 주목을 받지 못했으나, 2018년 이후 유튜브 알고리즘과 팬 페이지를 통해 재발견되며 폭발적인 인기를 얻었습니다.")
            .usageContext("예상치 못한 상황에서 터져 나오는 순수한 기쁨, 환희, 또는 놀라움을 표현할 때 사용됩니다. 때로는 상황이 다소 어이없거나 엉뚱할 때의 유쾌함을 나타내기도 합니다. 특정 인물이나 상황에 대한 비난 또는 칭찬의 맥락에서도 사용될 수 있으며, 전반적으로 긍정적이고 유쾌한 분위기를 조성하는 데 활용됩니다.")
            .trendPeriod("2018")
            .hashtags("[\"#무한도전\", \"#무야호\", \"#최규재\", \"#알래스카\", \"#인터넷밈\", \"#유행어\", \"#환희\", \"#기쁨\", \"#감탄사\"]")
            .flag(Meme.Flag.NORMAL)
            .build();

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
        PageResponse<Cursor, MemeDetailResponse> response = memeLookUpService.getMemesByCategory(0L, null, 1);

        // then
        then(response.paging()).extracting(Cursor::getNext, Cursor::isHasMore, Cursor::getPageSize)
            .containsExactly(무야호.getId(), true, 1);

        then(response.results()).hasSize(1)
            .extracting(MemeDetailResponse::title)
            .containsExactly("무야호");

        Long lastMemeId = response.paging().getNext();
        PageResponse<Cursor, MemeDetailResponse> nextPageResponse = memeLookUpService.getMemesByCategory(null, lastMemeId, 1);

        then(nextPageResponse.paging()).extracting(Cursor::getNext, Cursor::isHasMore, Cursor::getPageSize)
            .containsExactly(원영적_사고.getId(), true, 1);

        then(nextPageResponse.results()).hasSize(1)
            .extracting(MemeDetailResponse::title)
            .containsExactly("원영적 사고");
    }

}