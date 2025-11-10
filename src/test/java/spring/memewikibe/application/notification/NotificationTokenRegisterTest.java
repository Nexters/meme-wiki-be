package spring.memewikibe.application.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class NotificationTokenRegisterTest {

    private final NotificationTokenRegister sut;
    private final NotificationTokenRepository tokenRepository;

    NotificationTokenRegisterTest(NotificationTokenRegister sut,
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
        sut.registerToken(token);

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
        sut.registerToken(token1);
        sut.registerToken(token2);
        sut.registerToken(token3);

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
        sut.registerToken(token);
        sut.registerToken(token);

        // then
        then(tokenRepository.findAll())
            .hasSize(1)
            .extracting("token")
            .containsExactly(token);
    }

    @Test
    void 매우_긴_토큰도_등록할_수_있다() {
        // given
        // Firebase Cloud Messaging tokens are typically 152+ characters
        String longToken = "a".repeat(200);

        // when
        sut.registerToken(longToken);

        // then
        then(tokenRepository.findById(longToken)).isPresent();
    }

    @Test
    void 특수문자가_포함된_토큰을_등록할_수_있다() {
        // given
        String tokenWithSpecialChars = "token-with-:_.-special";

        // when
        sut.registerToken(tokenWithSpecialChars);

        // then
        then(tokenRepository.findById(tokenWithSpecialChars)).isPresent();
    }

    @Test
    void 기존_토큰을_재등록해도_DB에는_한개만_존재한다() {
        // given
        String token = "refresh-token";
        sut.registerToken(token);

        // when
        sut.registerToken(token); // Re-register same token

        // then
        then(tokenRepository.findAll()).hasSize(1);
        then(tokenRepository.findById(token)).isPresent();
    }
}
