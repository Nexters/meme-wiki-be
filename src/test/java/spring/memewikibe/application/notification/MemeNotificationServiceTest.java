package spring.memewikibe.application.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.NotificationTokenRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeNotificationServiceTest {

    private final MemeNotificationService sut;
    private final MemeRepository memeRepository;
    private final NotificationTokenRepository tokenRepository;

    @MockitoBean
    private NotificationSender notificationSender;

    MemeNotificationServiceTest(MemeNotificationService sut,
                                MemeRepository memeRepository,
                                NotificationTokenRepository tokenRepository) {
        this.sut = sut;
        this.memeRepository = memeRepository;
        this.tokenRepository = tokenRepository;
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    void 토큰이_없으면_전송을_시도하지_않고_조용히_종료한다() {
        // given
        Meme meme = Meme.builder()
            .title("무야호")
            .origin("origin")
            .usageContext("usage")
            .trendPeriod("2024")
            .imgUrl("https://example.com/meme.jpg")
            .hashtags("#무야호")
            .flag(Meme.Flag.NORMAL)
            .build();
        Long memeId = memeRepository.save(meme).getId();

        // when: 토큰 저장 없이 전송 호출
        sut.sendMemeNotification(memeId, "title", "body");

        // then: 발송 어댑터는 호출되지 않음
        verify(notificationSender, never()).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 존재하지_않는_밈이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> sut.sendMemeNotification(999999L, "t", "b"))
            .isInstanceOf(MemeWikiApplicationException.class)
            .satisfies(ex -> then(((MemeWikiApplicationException) ex).getErrorType())
                .isEqualTo(ErrorType.MEME_NOT_FOUND));
    }
}

