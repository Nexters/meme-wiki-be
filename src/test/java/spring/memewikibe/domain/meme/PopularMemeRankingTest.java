package spring.memewikibe.domain.meme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

@UnitTest
class PopularMemeRankingTest {

    @Test
    @DisplayName("viewed 이벤트가 score를 증가시킨다")
    void viewed_increments_score() {
        // given
        PopularMemeRankingConfig config = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(config);

        // when
        cache.viewed(1L);
        cache.viewed(1L);
        cache.viewed(1L);

        // then
        List<Long> topMemes = cache.getTopMemes(10);
        then(topMemes).containsExactly(1L);
    }

    @Test
    @DisplayName("shared 이벤트가 view보다 높은 가중치를 갖는다")
    void shared_has_higher_weight() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        cache.viewed(1L);   // 1점
        cache.viewed(1L);   // 1점
        cache.viewed(1L);   // 1점
        cache.shared(2L);   // 3점

        // then
        List<Long> topMemes = cache.getTopMemes(10);
        then(topMemes).containsExactly(2L, 1L);  // 같은 점수면 역순

        // 한 번 더 shared하면 2L이 더 높아짐
        cache.shared(2L);   // 3점 추가
        topMemes = cache.getTopMemes(10);
        then(topMemes).containsExactly(2L, 1L);  // 2L=6점 (1위), 1L=3점 (2위)
    }

    @Test
    @DisplayName("customized 이벤트가 적절한 가중치를 갖는다")
    void customized_weight() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        cache.viewed(1L);       // 1점
        cache.customized(2L);   // 2점
        cache.shared(3L);       // 3점

        // then
        List<Long> topMemes = cache.getTopMemes(10);
        then(topMemes).containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("여러 밈에 대한 이벤트를 처리하고 Top K를 반환한다")
    void multiple_memes_top_k() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        cache.viewed(1L);
        cache.viewed(1L);
        cache.viewed(1L);       // 1L = 3점

        cache.viewed(2L);
        cache.customized(2L);   // 2L = 3점

        cache.shared(3L);       // 3L = 3점

        cache.viewed(4L);
        cache.viewed(4L);
        cache.customized(4L);
        cache.shared(4L);       // 4L = 7점

        cache.shared(5L);
        cache.shared(5L);       // 5L = 6점

        // then - 높은 점수부터 (내림차순)
        List<Long> top3 = cache.getTopMemes(3);
        then(top3).hasSize(3);
        then(top3.get(0)).isEqualTo(4L);  // 7점 (1위)
        then(top3.get(1)).isEqualTo(5L);  // 6점 (2위)
        // 3위는 1L, 2L, 3L 중 하나 (모두 3점)
        then(top3.get(2)).isIn(1L, 2L, 3L);
    }

    @Test
    @DisplayName("getTopMemes에 count를 지정하면 해당 개수만큼만 반환한다")
    void get_top_memes_with_count() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 1.0, 1.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        for (long i = 1; i <= 10; i++) {
            cache.viewed(i);
        }

        // when
        List<Long> top5 = cache.getTopMemes(5);

        // then
        then(top5).hasSize(5);
    }

    @Test
    @DisplayName("size로 현재 캐시된 밈 개수를 확인할 수 있다")
    void size() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        cache.viewed(1L);
        cache.viewed(2L);
        cache.viewed(3L);

        // then
        then(cache.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("TTL이 지나면 밈이 캐시에서 제거된다")
    void ttl_expiration() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofMillis(100), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        cache.viewed(1L);
        cache.viewed(2L);

        // when & then
        await().atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(cache.getTopMemes(10)).isEmpty();
                then(cache.size()).isEqualTo(0);
            });
    }

    @Test
    @DisplayName("이벤트 발생 시 TTL이 갱신된다")
    void ttl_refresh() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofMillis(200), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        cache.viewed(1L);

        // when - 150ms 후 다시 viewed (TTL 갱신)
        await().pollDelay(Duration.ofMillis(150))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> cache.viewed(1L));

        // then - 100ms 더 기다려도 아직 만료되지 않음
        await().pollDelay(Duration.ofMillis(100))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(cache.getTopMemes(10)).containsExactly(1L);
            });
    }

    @Test
    @DisplayName("다양한 이벤트 타입을 혼합하여 처리할 수 있다")
    void mixed_event_types() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        cache.viewed(1L);
        cache.shared(1L);
        cache.customized(1L);   // 1L = 1 + 3 + 2 = 6점

        cache.viewed(2L);
        cache.viewed(2L);       // 2L = 2점

        // then
        List<Long> topMemes = cache.getTopMemes(10);
        then(topMemes).containsExactly(1L, 2L);  // 1L=6점, 2L=2점 (내림차순)
    }

    @Test
    @DisplayName("빈 캐시에서 getTopMemes를 호출하면 빈 리스트를 반환한다")
    void empty_cache() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 2.0, 3.0
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        List<Long> topMemes = cache.getTopMemes(10);

        // then
        then(topMemes).isEmpty();
    }

    @Test
    @DisplayName("커스텀 properties로 가중치를 조정할 수 있다")
    void custom_properties() {
        // given
        PopularMemeRankingConfig properties = new PopularMemeRankingConfig(
            Duration.ofHours(1), 1.0, 10.0, 100.0  // 가중치 크게 차이
        );
        PopularMemeRanking cache = new PopularMemeRanking(properties);

        // when
        for (int i = 0; i < 50; i++) {
            cache.viewed(1L);      // 1L = 50점
        }
        cache.customized(2L);      // 2L = 10점
        cache.shared(3L);          // 3L = 100점

        // then
        List<Long> topMemes = cache.getTopMemes(10);
        then(topMemes.get(0)).isEqualTo(3L);   // 100점
        then(topMemes.get(1)).isEqualTo(1L);   // 50점
        then(topMemes.get(2)).isEqualTo(2L);   // 10점
    }
}
