package spring.memewikibe.infrastructure;

import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.RepositoryTest;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeShareLog;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@RepositoryTest
class MemeShareLogRepositoryTest {

    private final MemeShareLogRepository sut;
    private final MemeRepository memeRepository;

    MemeShareLogRepositoryTest(MemeShareLogRepository sut, MemeRepository memeRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
    }

    @Test
    void 기간별_공유_횟수가_높은순으로_조회한다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        sut.saveAll(List.of(createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트),
            createMemeShareLog(무야호), createMemeShareLog(무야호),
            createMemeShareLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> result = sut.findTopMemesByShareCountWithin(Duration.ofDays(1), 2);
        // then
        then(result).hasSize(2)
            .extracting(MemeSimpleResponse::title)
            .containsExactlyInAnyOrder("전남친 토스트", "무야호");
    }

    @Test
    void findTopMemesByShareCountWithin_메서드는_동일한_카운트를_가질_때_id가_높은순으로_조회된다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        sut.saveAll(List.of(createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트), createMemeShareLog(전남친_토스트),
            createMemeShareLog(무야호), createMemeShareLog(무야호),
            createMemeShareLog(맑은_눈의_광인), createMemeShareLog(맑은_눈의_광인)
        ));

        // when
        List<MemeSimpleResponse> result = sut.findTopMemesByShareCountWithin(Duration.ofDays(1), 2);
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

    private MemeShareLog createMemeShareLog(Meme meme) {
        return MemeShareLog.of(meme);
    }
}