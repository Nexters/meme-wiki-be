package spring.memewikibe.domain.meme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.assertThatThrownBy;

@UnitTest
class MemeLogTest {

    @Test
    @DisplayName("MemeCustomLog.of()는 null meme을 받으면 NullPointerException을 던진다")
    void memeCustomLog_of_throwsNullPointerException_whenMemeIsNull() {
        // when & then
        assertThatThrownBy(() -> MemeCustomLog.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("meme must not be null");
    }

    @Test
    @DisplayName("MemeCustomLog.of()는 유효한 meme을 받으면 MemeCustomLog를 생성한다")
    void memeCustomLog_of_createsMemeCustomLog_whenMemeIsValid() {
        // given
        Meme meme = Meme.builder()
            .title("테스트 밈")
            .origin("테스트 기원")
            .usageContext("테스트 사용법")
            .trendPeriod("2024-01")
            .imgUrl("http://test.com/image.jpg")
            .hashtags("#테스트")
            .build();

        // when
        MemeCustomLog log = MemeCustomLog.of(meme);

        // then
        then(log).isNotNull();
        then(log.getMeme()).isEqualTo(meme);
    }

    @Test
    @DisplayName("MemeViewLog.of()는 null meme을 받으면 NullPointerException을 던진다")
    void memeViewLog_of_throwsNullPointerException_whenMemeIsNull() {
        // when & then
        assertThatThrownBy(() -> MemeViewLog.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("meme must not be null");
    }

    @Test
    @DisplayName("MemeViewLog.of()는 유효한 meme을 받으면 MemeViewLog를 생성한다")
    void memeViewLog_of_createsMemeViewLog_whenMemeIsValid() {
        // given
        Meme meme = Meme.builder()
            .title("테스트 밈")
            .origin("테스트 기원")
            .usageContext("테스트 사용법")
            .trendPeriod("2024-01")
            .imgUrl("http://test.com/image.jpg")
            .hashtags("#테스트")
            .build();

        // when
        MemeViewLog log = MemeViewLog.of(meme);

        // then
        then(log).isNotNull();
        then(log.getMeme()).isEqualTo(meme);
    }

    @Test
    @DisplayName("MemeShareLog.of()는 null meme을 받으면 NullPointerException을 던진다")
    void memeShareLog_of_throwsNullPointerException_whenMemeIsNull() {
        // when & then
        assertThatThrownBy(() -> MemeShareLog.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("meme must not be null");
    }

    @Test
    @DisplayName("MemeShareLog.of()는 유효한 meme을 받으면 MemeShareLog를 생성한다")
    void memeShareLog_of_createsMemeShareLog_whenMemeIsValid() {
        // given
        Meme meme = Meme.builder()
            .title("테스트 밈")
            .origin("테스트 기원")
            .usageContext("테스트 사용법")
            .trendPeriod("2024-01")
            .imgUrl("http://test.com/image.jpg")
            .hashtags("#테스트")
            .build();

        // when
        MemeShareLog log = MemeShareLog.of(meme);

        // then
        then(log).isNotNull();
        then(log.getMeme()).isEqualTo(meme);
    }
}
