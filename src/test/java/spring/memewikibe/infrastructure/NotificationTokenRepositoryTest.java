package spring.memewikibe.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.notification.NotificationToken;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class NotificationTokenRepositoryTest {

    private final NotificationTokenRepository sut;

    NotificationTokenRepositoryTest(NotificationTokenRepository sut) {
        this.sut = sut;
    }

    @AfterEach
    void tearDown() {
        sut.deleteAllInBatch();
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
}

