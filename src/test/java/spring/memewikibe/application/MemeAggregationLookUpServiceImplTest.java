package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeShareLog;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeAggregationLookUpServiceImplTest {

    private final MemeAggregationLookUpServiceImpl sut;
    private final MemeRepository memeRepository;
    private final MemeShareLogRepository shareLogRepository;

    MemeAggregationLookUpServiceImplTest(MemeAggregationLookUpServiceImpl sut, MemeRepository memeRepository, MemeShareLogRepository shareLogRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.shareLogRepository = shareLogRepository;
    }

    @AfterEach
    void tearDown() {
        shareLogRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    void 공유수가_높은_밈을_조회한다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        shareLogRepository.saveAll(List.of(
            createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트),
            createMemeShareLog(무야호), createMemeShareLog(무야호),
            createMemeShareLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentSharedMemes();

        // then
        then(response).hasSize(3)
            .extracting(MemeSimpleResponse::title)
            .containsExactly("전남친 토스트", "무야호", "맑은 눈의 광인");
    }

    @Test
    void 동일한_공유수일_때_id가_높은순으로_정렬된다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트));

        shareLogRepository.saveAll(List.of(
            createMemeShareLog(무야호), createMemeShareLog(무야호),
            createMemeShareLog(나만_아니면_돼), createMemeShareLog(나만_아니면_돼),
            createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트)
        ));

        // when
        List<MemeSimpleResponse> response = sut.getMostFrequentSharedMemes();

        // then
        then(response).hasSize(3);
        // ID가 높은 순서로 정렬되어야 함 (저장 순서의 역순)
        then(response.get(0).title()).isEqualTo("전남친 토스트");
        then(response.get(1).title()).isEqualTo("나만 아니면 돼");
        then(response.get(2).title()).isEqualTo("무야호");
    }

    private Meme createMeme(String name) {
        return Meme.builder()
            .title(name)
            .origin("origin_" + name)
            .usageContext("usageContext_" + name)
            .trendPeriod("trendPeriod_" + name)
            .imgUrl("imgUrl_" + name)
            .hashtags("#" + name)
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
}