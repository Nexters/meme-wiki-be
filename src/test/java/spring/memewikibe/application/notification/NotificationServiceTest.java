package spring.memewikibe.application.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "cloudflare.r2.access-key-id=dummy",
    "cloudflare.r2.secret-access-key=dummy",
    "cloudflare.r2.endpoint=http://localhost",
    "cloudflare.r2.bucket-name=test-bucket"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class NotificationServiceTest {

    private final NotificationService sut;
    private final NotificationTokenRepository tokenRepository;

    NotificationServiceTest(NotificationService sut,
                            NotificationTokenRepository tokenRepository) {
        this.sut = sut;
        this.tokenRepository = tokenRepository;
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAllInBatch();
    }

    @Test
    void 푸시_알림_토큰을_등록한다() {
        // given
        String token = "token-123";

        // when
        sut.registerNotificationToken(token);

        // then
        then(tokenRepository.findAll())
            .hasSize(1)
            .extracting("token")
            .containsExactly(token);
    }

    @Test
    void 여러_푸시_알림_토큰을_등록한다() {
        // given
        String token1 = "token-a";
        String token2 = "token-b";
        String token3 = "token-c";

        // when
        sut.registerNotificationToken(token1);
        sut.registerNotificationToken(token2);
        sut.registerNotificationToken(token3);

        // then
        then(tokenRepository.findAll())
            .hasSize(3)
            .extracting("token")
            .containsExactlyInAnyOrder(token1, token2, token3);
    }

    @Test
    void 동일_토큰을_두번_등록해도_한건만_저장된다() {
        // given
        String token = "dup-token";

        // when
        sut.registerNotificationToken(token);
        sut.registerNotificationToken(token);

        // then
        then(tokenRepository.findAll())
            .hasSize(1)
            .extracting("token")
            .containsExactly(token);
    }

    @Test
    void 빈_토큰은_등록할_수_없다() {
        // given
        String emptyToken = "";

        // when
        sut.registerNotificationToken(emptyToken);

        // then
        then(tokenRepository.findAll()).isEmpty();
    }

    @Test
    void 공백만_있는_토큰은_등록할_수_없다() {
        // given
        String blankToken = "   ";

        // when
        sut.registerNotificationToken(blankToken);

        // then
        then(tokenRepository.findAll()).isEmpty();
    }
}
