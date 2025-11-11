package spring.memewikibe.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.annotation.IntegrationTest;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCustomLog;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@IntegrationTest
@Transactional
class MemeCustomLogRepositoryTest {

    private final MemeCustomLogRepository sut;
    private final MemeRepository memeRepository;

    MemeCustomLogRepositoryTest(MemeCustomLogRepository sut, MemeRepository memeRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
    }

    @AfterEach
    void tearDown() {
        sut.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    void 기간별_커스텀_횟수가_높은순으로_조회한다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        sut.saveAll(List.of(createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트),
            createMemeCustomLog(무야호), createMemeCustomLog(무야호),
            createMemeCustomLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> result = sut.findTopMemesByCustomCountWithin(Duration.ofDays(1), 2);

        // then
        then(result).hasSize(2)
            .extracting(MemeSimpleResponse::title)
            .containsExactlyInAnyOrder("전남친 토스트", "무야호");
    }

    @Test
    void findTopMemesByCustomCountWithin_메서드는_동일한_카운트를_가질_때_id가_높은순으로_조회된다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        sut.saveAll(List.of(createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트), createMemeCustomLog(전남친_토스트),
            createMemeCustomLog(무야호), createMemeCustomLog(무야호),
            createMemeCustomLog(맑은_눈의_광인), createMemeCustomLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> result = sut.findTopMemesByCustomCountWithin(Duration.ofDays(1), 2);

        // then
        then(result).hasSize(2)
            .extracting(MemeSimpleResponse::title)
            .containsExactlyInAnyOrder("전남친 토스트", "맑은 눈의 광인");
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

    private MemeCustomLog createMemeCustomLog(Meme meme) {
        return MemeCustomLog.builder()
            .meme(meme)
            .build();
    }
}