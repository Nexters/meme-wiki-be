package spring.memewikibe.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.*;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeAggregationRepositoryTest {

    private final MemeAggregationRepository sut;
    private final MemeRepository memeRepository;
    private final MemeCustomLogRepository customLogRepository;
    private final MemeShareLogRepository shareLogRepository;
    private final MemeViewLogRepository viewLogRepository;

    MemeAggregationRepositoryTest(MemeAggregationRepository memeAggregationRepository, MemeAggregationRepository sut, MemeRepository memeRepository,
                                  MemeCustomLogRepository customLogRepository,
                                  MemeShareLogRepository shareLogRepository,
                                  MemeViewLogRepository viewLogRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.customLogRepository = customLogRepository;
        this.shareLogRepository = shareLogRepository;
        this.viewLogRepository = viewLogRepository;
    }

    @AfterEach
    void tearDown() {
        customLogRepository.deleteAllInBatch();
        shareLogRepository.deleteAllInBatch();
        viewLogRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    void 가중치를_적용한_인기도순으로_밈을_조회한다() {
        // given
        // 가중치 적용: custom 3점, share 2점, view 1점
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        // 전남친 토스트: custom 1개, share 2개, view 1개 = 8점
        customLogRepository.save(createMemeCustomLog(전남친_토스트));
        shareLogRepository.saveAll(List.of(createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트)));
        viewLogRepository.save(createMemeViewLog(전남친_토스트));

        // 무야호: custom 2개, view 1개 = 7점
        customLogRepository.saveAll(List.of(createMemeCustomLog(무야호), createMemeCustomLog(무야호)));
        viewLogRepository.save(createMemeViewLog(무야호));

        // 나만 아니면 돼: share 1개, view 3개 = 6점
        shareLogRepository.save(createMemeShareLog(나만_아니면_돼));
        viewLogRepository.saveAll(List.of(createMemeViewLog(나만_아니면_돼), createMemeViewLog(나만_아니면_돼), createMemeViewLog(나만_아니면_돼)));

        // 맑은 눈의 광인: custom 3개 = 9점
        customLogRepository.saveAll(List.of(createMemeCustomLog(맑은_눈의_광인), createMemeCustomLog(맑은_눈의_광인), createMemeCustomLog(맑은_눈의_광인)));

        // when
        List<MemeAggregationResult> result = sut.findTopRatedMemesBy(Duration.ofDays(1), 3);

        then(result).hasSize(3)
            .extracting(MemeAggregationResult::title)
            .containsExactly("맑은 눈의 광인", "전남친 토스트", "무야호");
    }

    @Test
    void 동일한_가중치_점수일때_id가_높은순으로_조회한다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼));

        // 둘 다 custom 1개씩 = 3점
        customLogRepository.saveAll(List.of(createMemeCustomLog(무야호), createMemeCustomLog(나만_아니면_돼)));

        // when
        List<MemeAggregationResult> result = sut.findTopRatedMemesBy(Duration.ofDays(1), 2);

        // then - 동점일 때 id가 높은 순 (나만_아니면_돼가 나중에 생성되어 id가 더 큼)
        then(result).hasSize(2)
            .extracting(MemeAggregationResult::title)
            .containsExactly("나만 아니면 돼", "무야호");
    }

    @Test
    void 로그가_없는_밈도_결과에_포함된다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 로그없는밈 = createMeme("로그없는밈");

        memeRepository.saveAll(List.of(무야호, 로그없는밈));

        // 무야호만 로그 추가
        customLogRepository.save(createMemeCustomLog(무야호));

        // when
        List<MemeAggregationResult> result = sut.findTopRatedMemesBy(Duration.ofDays(1), 5);

        // then - 로그 없는 밈도 포함 (0점으로 처리)
        then(result).hasSize(2)
            .extracting(MemeAggregationResult::title)
            .containsExactly("무야호", "로그없는밈");
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

    private MemeCustomLog createMemeCustomLog(Meme meme) {
        return MemeCustomLog.builder()
            .meme(meme)
            .build();
    }

    private MemeShareLog createMemeShareLog(Meme meme) {
        return MemeShareLog.builder()
            .meme(meme)
            .build();
    }

    private MemeViewLog createMemeViewLog(Meme meme) {
        return MemeViewLog.builder()
            .meme(meme)
            .build();
    }
}