package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCustomLog;
import spring.memewikibe.domain.meme.MemeShareLog;
import spring.memewikibe.domain.meme.MemeViewLog;
import spring.memewikibe.infrastructure.MemeCustomLogRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;
import spring.memewikibe.infrastructure.MemeViewLogRepository;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeAggregationLookUpServiceImplTest {

    private final MemeAggregationLookUpServiceImpl sut;
    private final MemeRepository memeRepository;
    private final MemeShareLogRepository shareLogRepository;
    private final MemeCustomLogRepository customLogRepository;
    private final MemeViewLogRepository viewLogRepository;

    MemeAggregationLookUpServiceImplTest(MemeAggregationLookUpServiceImpl sut, MemeRepository memeRepository, MemeShareLogRepository shareLogRepository, MemeCustomLogRepository customLogRepository, MemeViewLogRepository viewLogRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.shareLogRepository = shareLogRepository;
        this.customLogRepository = customLogRepository;
        this.viewLogRepository = viewLogRepository;
    }

    @AfterEach
    void tearDown() {
        shareLogRepository.deleteAllInBatch();
        customLogRepository.deleteAllInBatch();
        viewLogRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    void 공유수가_높은_밈을_조회한다() {
        // given
        List<Meme> memes = createMemes(15); // fallback용 추가 밈들
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(memes);
        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        shareLogRepository.saveAll(List.of(
            createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트),
            createMemeShareLog(무야호), createMemeShareLog(무야호),
            createMemeShareLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentSharedMemes();

        // then
        then(response).hasSize(10);
        // 공유수가 높은 밈들이 상위에 위치
        then(response.get(0).title()).isEqualTo("전남친 토스트");
        then(response.get(1).title()).isEqualTo("무야호");
        then(response.get(2).title()).isEqualTo("맑은 눈의 광인");
        // 나머지는 최신 밈들로 채워짐 (fallback 동작 확인)
        then(response).extracting(MemeSimpleResponse::title)
            .contains("전남친 토스트", "무야호", "맑은 눈의 광인");
    }

    @Test
    void 동일한_공유수일_때_id가_높은순으로_정렬된다() {
        // given
        List<Meme> memes = createMemes(15); // fallback용 추가 밈들
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");

        memeRepository.saveAll(memes);
        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트));

        shareLogRepository.saveAll(List.of(
            createMemeShareLog(무야호), createMemeShareLog(무야호),
            createMemeShareLog(나만_아니면_돼), createMemeShareLog(나만_아니면_돼),
            createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트)
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentSharedMemes();

        // then
        then(response).hasSize(10);
        // 동일한 공유수일 때 id가 높은순으로 정렬
        then(response.get(0).title()).isEqualTo("전남친 토스트");
        then(response.get(1).title()).isEqualTo("나만 아니면 돼");
        then(response.get(2).title()).isEqualTo("무야호");
        // 나머지는 최신 밈들로 채워짐 (fallback 동작 확인)
        then(response).extracting(MemeSimpleResponse::title)
            .contains("전남친 토스트", "나만 아니면 돼", "무야호");
    }

    @Test
    void 커스텀수가_높은_밈을_조회한다() {
        // given
        List<Meme> memes = createMemes(15); // fallback용 추가 밈들
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(memes);
        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        customLogRepository.saveAll(List.of(
            createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트),
            createMemeCustomLog(무야호), createMemeCustomLog(무야호),
            createMemeCustomLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentCustomMemes();

        // then
        then(response).hasSize(10);
        // 커스텀수가 높은 밈들이 상위에 위치
        then(response.get(0).title()).isEqualTo("전남친 토스트");
        then(response.get(1).title()).isEqualTo("무야호");
        then(response.get(2).title()).isEqualTo("맑은 눈의 광인");
        // 나머지는 최신 밈들로 채워짐 (fallback 동작 확인)
        then(response).extracting(MemeSimpleResponse::title)
            .contains("전남친 토스트", "무야호", "맑은 눈의 광인");
    }

    @Test
    void 동일한_커스텀수일_때_id가_높은순으로_정렬된다() {
        // given
        List<Meme> memes = createMemes(15); // fallback용 추가 밈들
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");

        memeRepository.saveAll(memes);
        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트));

        // 모든 밈이 동일하게 2개씩 커스텀
        customLogRepository.saveAll(List.of(
            createMemeCustomLog(무야호), createMemeCustomLog(무야호),
            createMemeCustomLog(나만_아니면_돼), createMemeCustomLog(나만_아니면_돼),
            createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트)
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentCustomMemes();

        // then
        then(response).hasSize(10);
        // 동일한 커스텀수일 때 id가 높은순으로 정렬
        then(response.get(0).title()).isEqualTo("전남친 토스트");
        then(response.get(1).title()).isEqualTo("나만 아니면 돼");
        then(response.get(2).title()).isEqualTo("무야호");
        // 나머지는 최신 밈들로 채워짐 (fallback 동작 확인)
        then(response).extracting(MemeSimpleResponse::title)
            .contains("전남친 토스트", "나만 아니면 돼", "무야호");
    }

    @Test
    void 공유_로그가_부족할_때_최신_밈으로_채워서_반환한다() {
        // given
        List<Meme> memes = createMemes(15);
        memeRepository.saveAll(memes);

        // 공유 로그는 2개만 생성 (목표: 10개)
        shareLogRepository.saveAll(List.of(
            createMemeShareLog(memes.get(0)),
            createMemeShareLog(memes.get(1))
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentSharedMemes();

        // then
        then(response).hasSize(10);
        // 첫 2개는 공유 로그가 있는 밈들 (id 역순)
        then(response.get(0).title()).isEqualTo("밈_1");
        then(response.get(1).title()).isEqualTo("밈_0");
        // 나머지 8개는 최신 밈들로 채워짐 (공유 로그 밈 제외)
        then(response.subList(2, 10)).extracting(MemeSimpleResponse::title)
            .containsExactly("밈_14", "밈_13", "밈_12", "밈_11", "밈_10", "밈_9", "밈_8", "밈_7");
    }

    @Test
    void 공유_로그가_없을_때_최신_밈_10개를_반환한다() {
        // given
        List<Meme> memes = createMemes(15);
        memeRepository.saveAll(memes);

        // when (공유 로그 없음)
        List<MemeSimpleResponse> response = sut.getMostFrequentSharedMemes();

        // then
        then(response).hasSize(10);
        then(response).extracting(MemeSimpleResponse::title)
            .containsExactly("밈_14", "밈_13", "밈_12", "밈_11", "밈_10", "밈_9", "밈_8", "밈_7", "밈_6", "밈_5");
    }

    @Test
    void 커스텀_로그가_부족할_때_최신_밈으로_채워서_반환한다() {
        // given
        List<Meme> memes = createMemes(15);
        memeRepository.saveAll(memes);

        // 커스텀 로그는 3개만 생성 (목표: 10개)
        customLogRepository.saveAll(List.of(
            createMemeCustomLog(memes.get(0)),
            createMemeCustomLog(memes.get(1)),
            createMemeCustomLog(memes.get(2))
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentCustomMemes();

        // then
        then(response).hasSize(10);
        // 첫 3개는 커스텀 로그가 있는 밈들 (id 역순)
        then(response.get(0).title()).isEqualTo("밈_2");
        then(response.get(1).title()).isEqualTo("밈_1");
        then(response.get(2).title()).isEqualTo("밈_0");
        // 나머지 7개는 최신 밈들로 채워짐 (커스텀 로그 밈 제외)
        then(response.subList(3, 10)).extracting(MemeSimpleResponse::title)
            .containsExactly("밈_14", "밈_13", "밈_12", "밈_11", "밈_10", "밈_9", "밈_8");
    }

    @Test
    void 커스텀_로그가_없을_때_최신_밈_10개를_반환한다() {
        // given
        List<Meme> memes = createMemes(15);
        memeRepository.saveAll(memes);

        // when (커스텀 로그 없음)
        List<MemeSimpleResponse> response = sut.getMostFrequentCustomMemes();

        // then
        then(response).hasSize(10);
        then(response).extracting(MemeSimpleResponse::title)
            .containsExactly("밈_14", "밈_13", "밈_12", "밈_11", "밈_10", "밈_9", "밈_8", "밈_7", "밈_6", "밈_5");
    }

    @Test
    void 인기_점수가_부족할_때_최신_밈으로_채워서_반환한다() {
        // given
        List<Meme> memes = createMemes(10);
        memeRepository.saveAll(memes);

        // 인기도 점수를 위한 로그들을 일부 밈에만 생성 (목표: 6개)
        // 밈_0에 높은 점수 (커스텀3 + 공유2 + 조회1 = 3*3 + 2*2 + 1*1 = 14점)
        customLogRepository.saveAll(List.of(
            createMemeCustomLog(memes.get(0)), createMemeCustomLog(memes.get(0)), createMemeCustomLog(memes.get(0))
        ));
        shareLogRepository.saveAll(List.of(
            createMemeShareLog(memes.get(0)), createMemeShareLog(memes.get(0))
        ));
        viewLogRepository.saveAll(List.of(
            createMemeViewLog(memes.get(0))
        ));

        // 밈_1에 중간 점수 (커스텀1 + 공유1 = 1*3 + 1*2 = 5점)
        customLogRepository.saveAll(List.of(
            createMemeCustomLog(memes.get(1))
        ));
        shareLogRepository.saveAll(List.of(
            createMemeShareLog(memes.get(1))
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostPopularMemes();

        // then
        then(response).hasSize(6);
        // 첫 2개는 인기도 점수가 있는 밈들 (점수 순)
        then(response.get(0).title()).isEqualTo("밈_0");
        then(response.get(1).title()).isEqualTo("밈_1");
        // 나머지 4개는 최신 밈들로 채워짐 (인기도 점수 밈 제외)
        then(response.subList(2, 6)).extracting(MemeSimpleResponse::title)
            .containsExactly("밈_9", "밈_8", "밈_7", "밈_6");
    }

    @Test
    void 인기_점수가_없을_때_최신_밈_6개를_반환한다() {
        // given
        List<Meme> memes = createMemes(10);
        memeRepository.saveAll(memes);

        // when (인기도 관련 로그 없음)
        List<MemeSimpleResponse> response = sut.getMostPopularMemes();

        // then
        then(response).hasSize(6);
        then(response).extracting(MemeSimpleResponse::title)
            .containsExactly("밈_9", "밈_8", "밈_7", "밈_6", "밈_5", "밈_4");
    }

    private Meme createMeme(String name) {
        return Meme.builder()
            .title(name)
            .origin("origin_" + name)
            .usageContext("usageContext_" + name)
            .trendPeriod("trendPeriod_" + name)
            .imgUrl("imgUrl_" + name)
            .hashtags("#" + name)
            .flag(Meme.Flag.NORMAL)
            .build();
    }

    private List<Meme> createMemes(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createMeme("밈_" + i))
            .toList();
    }

    private MemeShareLog createMemeShareLog(Meme meme) {
        return MemeShareLog.builder()
            .meme(meme)
            .build();
    }

    private MemeCustomLog createMemeCustomLog(Meme meme) {
        return MemeCustomLog.builder()
            .meme(meme)
            .build();
    }

    private MemeViewLog createMemeViewLog(Meme meme) {
        return MemeViewLog.builder()
            .meme(meme)
            .build();
    }
}