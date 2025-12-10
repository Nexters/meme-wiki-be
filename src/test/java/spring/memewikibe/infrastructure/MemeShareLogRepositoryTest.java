package spring.memewikibe.infrastructure;

import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.RepositoryTest;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeShareLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@RepositoryTest
class MemeShareLogRepositoryTest {

    private final MemeShareLogRepository sut;
    private final MemeRepository memeRepository;
    private final jakarta.persistence.EntityManager entityManager;

    MemeShareLogRepositoryTest(MemeShareLogRepository sut, MemeRepository memeRepository, jakarta.persistence.EntityManager entityManager) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.entityManager = entityManager;
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

    @Test
    void 기간_외의_공유_로그는_제외하고_조회한다() {
        // given
        Meme 무야호 = createMeme("무야호");
        Meme 나만_아니면_돼 = createMeme("나만 아니면 돼");
        Meme 전남친_토스트 = createMeme("전남친 토스트");
        Meme 맑은_눈의_광인 = createMeme("맑은 눈의 광인");

        memeRepository.saveAll(List.of(무야호, 나만_아니면_돼, 전남친_토스트, 맑은_눈의_광인));

        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        LocalDateTime eightDaysAgo = LocalDateTime.now().minusDays(8);

        // 최근 2일 이내의 공유 로그
        List<MemeShareLog> recentLogs = sut.saveAll(List.of(
            createMemeShareLog(전남친_토스트),
            createMemeShareLog(전남친_토스트),
            createMemeShareLog(무야호)
        ));

        // 8일 전의 공유 로그 (Duration.ofDays(7) 범위 외)
        List<MemeShareLog> oldLogs = sut.saveAll(List.of(
            createMemeShareLog(나만_아니면_돼),
            createMemeShareLog(나만_아니면_돼),
            createMemeShareLog(나만_아니면_돼),
            createMemeShareLog(맑은_눈의_광인)
        ));

        // 네이티브 SQL을 사용하여 createdAt 직접 업데이트
        entityManager.flush();
        for (MemeShareLog log : recentLogs) {
            entityManager.createNativeQuery("UPDATE meme_share_log SET created_at = ? WHERE id = ?")
                .setParameter(1, twoDaysAgo)
                .setParameter(2, log.getId())
                .executeUpdate();
        }
        for (MemeShareLog log : oldLogs) {
            entityManager.createNativeQuery("UPDATE meme_share_log SET created_at = ? WHERE id = ?")
                .setParameter(1, eightDaysAgo)
                .setParameter(2, log.getId())
                .executeUpdate();
        }
        entityManager.flush();
        entityManager.clear();

        // when: 7일 이내의 공유 로그만 조회
        List<MemeSimpleResponse> result = sut.findTopMemesByShareCountWithin(Duration.ofDays(7), 10);

        // then: 8일 전 로그는 제외되고, 2일 전 로그만 포함됨
        then(result).hasSize(2)
            .extracting(MemeSimpleResponse::title)
            .containsExactly("전남친 토스트", "무야호"); // 전남친 토스트(2회) > 무야호(1회)
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
        return MemeShareLog.builder()
            .meme(meme)
            .build();
    }
}