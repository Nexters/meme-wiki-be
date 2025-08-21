package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
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
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;
import spring.memewikibe.support.response.Cursor;
import spring.memewikibe.support.response.PageResponse;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@Transactional
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeLookUpServiceImplTest {

    private final MemeLookUpServiceImpl memeLookUpService;
    private final CategoryRepository categoryRepository;
    private final MemeRepository memeRepository;
    private final MemeCategoryRepository memeCategoryRepository;

    MemeLookUpServiceImplTest(MemeLookUpServiceImpl memeLookUpService, CategoryRepository categoryRepository, MemeRepository memeRepository, MemeCategoryRepository memeCategoryRepository) {
        this.memeLookUpService = memeLookUpService;
        this.categoryRepository = categoryRepository;
        this.memeRepository = memeRepository;
        this.memeCategoryRepository = memeCategoryRepository;
    }

    @AfterEach
    void tearDown() {
        memeCategoryRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
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

    @Test
    void 카테고리별_밈을_조회한다() {
        // given
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
        memeCategoryRepository.saveAll(List.of(
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
        PageResponse<Cursor, MemeDetailResponse> response = memeLookUpService.getMemesByCategory(예능.getId(), null, 1);
        // then
        then(response.getPaging()).extracting(Cursor::getNext, Cursor::isHasMore, Cursor::getPageSize)
            .containsExactlyInAnyOrder(무야호.getId(), true, 1);
        then(response.getPaging().getNext()).isNotNull();
        then(response.getResults()).hasSize(1)
            .extracting(MemeDetailResponse::title)
            .containsExactlyInAnyOrder("무야호");
    }

    @Test
    void 쿼리로_밈을_검색한다_페이지네이션() {
        // given
        // ID 순서 보장을 위해 순서대로 저장
        Meme 밈1 = memeRepository.save(Meme.builder().title("테스트 밈").flag(Meme.Flag.NORMAL).build());
        Meme 밈2 = memeRepository.save(Meme.builder().title("테스트 밈입니다").flag(Meme.Flag.NORMAL).build());
        Meme 밈3 = memeRepository.save(Meme.builder().title("이것도 테스트 밈").flag(Meme.Flag.NORMAL).build());


        // when
        // "테스트"가 포함된 밈을 검색, 페이지 크기는 2
        PageResponse<Cursor, MemeDetailResponse> firstPage = memeLookUpService.getMemesByQuery("테스트", null, 2);

        // then: 첫 페이지 검증 (최신순: 밈3, 밈2)
        then(firstPage.getPaging()).extracting(Cursor::getNext, Cursor::isHasMore, Cursor::getPageSize)
            .containsExactlyInAnyOrder(밈2.getId(), true, 2);
        then(firstPage.getResults())
            .hasSize(2)
            .extracting(MemeDetailResponse::title)
            .containsExactlyInAnyOrder("이것도 테스트 밈", "테스트 밈입니다");

        // when: 두 번째 페이지 요청
        PageResponse<Cursor, MemeDetailResponse> secondPage = memeLookUpService.getMemesByQuery("테스트", firstPage.getPaging().getNext(), 2);

        // then: 두 번째 페이지 검증 (나머지: 밈1)
        then(secondPage.getPaging()).extracting(Cursor::getNext, Cursor::isHasMore, Cursor::getPageSize)
            .containsExactlyInAnyOrder(null, false, 1);
        then(secondPage.getResults())
            .hasSize(1)
            .extracting(MemeDetailResponse::title)
            .containsExactlyInAnyOrder("테스트 밈");
    }

    @Test
    void 검색어가_널값이면_전부_조회된다() {
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

        // when
        PageResponse<Cursor, MemeDetailResponse> response = memeLookUpService.getMemesByQuery(null, null, 10);

        // then
        then(response.getResults()).hasSize(3)
            .extracting(MemeDetailResponse::title)
            .containsExactlyInAnyOrder("나만 아니면 돼", "원영적 사고", "무야호");
    }

    @Test
    void 쿼리로_밈을_검색한다_결과_없음() {
        // given
        Meme 나만_아니면_돼 = Meme.builder().title("나만 아니면 돼").flag(Meme.Flag.NORMAL).build();
        memeRepository.save(나만_아니면_돼);

        // when
        PageResponse<Cursor, MemeDetailResponse> response = memeLookUpService.getMemesByQuery("없는검색어", null, 10);

        // then
        then(response.getResults()).isEmpty();
        then(response.getPaging().isHasMore()).isFalse();
        then(response.getPaging().getNext()).isNull();
    }

    @Test
    void 밈을_단건_조회한다() {
        // given
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

        // when
        MemeDetailResponse response = memeLookUpService.getMemeById(나만_아니면_돼.getId());

        // then
        then(response).extracting(MemeDetailResponse::title, MemeDetailResponse::origin, MemeDetailResponse::usageContext, MemeDetailResponse::trendPeriod, MemeDetailResponse::hashtags)
            .containsExactly("나만 아니면 돼",
                "KBS 2TV '1박 2일'의 복불복 게임에서 벌칙에 걸리지 않은 멤버들이 외치던 말에서 유래했습니다. 초창기 멤버 노홍철이 처음 사용했으며, 이후 강호동이 과장된 표정과 액션으로 외치며 짤방으로 널리 퍼졌습니다. 무한도전의 '무한이기주의'와 비교되며 승리자의 자축을 넘어 타인의 불행을 조롱하는 의미로 변모했습니다.",
                "원래는 복불복에서 살아남은 자의 자축이었으나, 시간이 지나며 타인의 불행을 보고 자신은 괜찮다며 조롱하는 용법으로 사용됩니다. 인터넷 커뮤니티에서 고소 사건, 단체 손해, 가해자 특정, 지탄받는 대상의 천벌 등 남의 불행에 연루되지 않은 사람들이 이를 비웃을 때 쓰입니다. 2020년대 이후 '알빠노' 등 극단적 이기주의 밈의 시초격으로 인식됩니다.",
                "2020",
                List.of("#1박2일", "#복불복", "#노홍철", "#강호동", "#이기주의", "#남의불행", "#짤방", "#인터넷밈", "#알빠노")
            );
    }

    @Test
    void ABNORMAL_밈은_조회할_수_없다() {
        // given
        Meme normal밈 = Meme.builder()
            .title("정상 밈")
            .origin("정상 밈의 출처")
            .usageContext("정상 밈의 사용맥락")
            .trendPeriod("2024")
            .hashtags("[\"#정상\"]")
            .flag(Meme.Flag.NORMAL)
            .build();
        Meme abnormal밈 = Meme.builder()
            .title("비정상 밈")
            .origin("비정상 밈의 출처")
            .usageContext("비정상 밈의 사용맥락")
            .trendPeriod("2024")
            .hashtags("[\"#비정상\"]")
            .flag(Meme.Flag.ABNORMAL)
            .build();
        
        memeRepository.saveAll(List.of(normal밈, abnormal밈));

        // when & then: NORMAL 밈은 조회 가능
        MemeDetailResponse normalResponse = memeLookUpService.getMemeById(normal밈.getId());
        then(normalResponse.title()).isEqualTo("정상 밈");
        
        // when & then: ABNORMAL 밈은 조회 불가 (MEME_NOT_FOUND 예외 발생)
        thenThrownBy(() -> memeLookUpService.getMemeById(abnormal밈.getId()))
            .isInstanceOf(MemeWikiApplicationException.class)
            .extracting(exception -> ((MemeWikiApplicationException) exception).getErrorType())
            .isEqualTo(ErrorType.MEME_NOT_FOUND);
    }
    
    @Test
    void 카테고리별_밈_조회시_ABNORMAL_밈은_제외된다() {
        // given
        Category 테스트카테고리 = Category.builder()
            .name("테스트")
            .imgUrl("https://example.com/test.jpg")
            .build();
        categoryRepository.save(테스트카테고리);

        Meme normal밈 = Meme.builder()
            .title("정상 밈")
            .origin("정상 밈의 출처")
            .usageContext("정상 밈의 사용맥락")
            .trendPeriod("2024")
            .hashtags("[\"#정상\"]")
            .flag(Meme.Flag.NORMAL)
            .build();
        Meme abnormal밈 = Meme.builder()
            .title("비정상 밈")
            .origin("비정상 밈의 출처")
            .usageContext("비정상 밈의 사용맥락")
            .trendPeriod("2024")
            .hashtags("[\"#비정상\"]")
            .flag(Meme.Flag.ABNORMAL)
            .build();
        
        memeRepository.saveAll(List.of(normal밈, abnormal밈));
        
        // 카테고리 연결
        memeCategoryRepository.saveAll(List.of(
            MemeCategory.builder().category(테스트카테고리).meme(normal밈).build(),
            MemeCategory.builder().category(테스트카테고리).meme(abnormal밈).build()
        ));

        // when
        PageResponse<Cursor, MemeDetailResponse> response = memeLookUpService.getMemesByCategory(테스트카테고리.getId(), null, 10);

        // then: NORMAL 밈만 조회됨
        then(response.getResults()).hasSize(1)
            .extracting(MemeDetailResponse::title)
            .containsExactly("정상 밈");
    }

    @Test
    void 존재하지_않는_밈을_조회하면_예외를_발생한다() {
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

        // when & then
        thenThrownBy(() -> memeLookUpService.getMemeById(999L))
            .isInstanceOf(MemeWikiApplicationException.class)
            .extracting(exception -> ((MemeWikiApplicationException) exception).getErrorType())
            .isEqualTo(ErrorType.MEME_NOT_FOUND);
    }
}