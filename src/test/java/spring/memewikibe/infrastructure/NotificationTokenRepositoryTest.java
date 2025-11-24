package spring.memewikibe.infrastructure;

import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.RepositoryTest;
import spring.memewikibe.domain.notification.NotificationToken;

import static org.assertj.core.api.BDDAssertions.then;

@RepositoryTest
class NotificationTokenRepositoryTest {

    private final NotificationTokenRepository sut;

    NotificationTokenRepositoryTest(NotificationTokenRepository sut) {
        this.sut = sut;
    }

    @Test
    void 토큰을_저장하면_생성시간과_수정시간이_채워진다() {
        // given
        NotificationToken token = NotificationToken.create("token-xyz");

        // when
        NotificationToken saved = sut.saveAndFlush(token);

        // then
        then(saved.getToken()).isEqualTo("token-xyz");
        then(saved.getCreatedAt()).isNotNull();
        then(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void 모든_토큰_문자열을_조회할_수_있다() {
        // given
        sut.save(NotificationToken.create("token1"));
        sut.save(NotificationToken.create("token2"));
        sut.save(NotificationToken.create("token3"));
        sut.flush();

        // when
        var tokens = sut.findAllTokens();

        // then
        then(tokens).containsExactlyInAnyOrder("token1", "token2", "token3");
    }
}

