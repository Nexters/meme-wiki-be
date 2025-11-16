package spring.memewikibe.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

@UnitTest
class TtlZsetTest {

    @Test
    @DisplayName("zincrby로 score를 증가시킬 수 있다")
    void zincrby_increment() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        zset.zincrby("key1", 10.0);

        // when
        zset.zincrby("key1", 5.0);
        zset.zincrby("key1", 3.0);

        // then
        then(zset.zscore("key1")).isEqualTo(18.0);
    }

    @Test
    @DisplayName("zincrby는 존재하지 않는 key에 대해 0에서 시작한다")
    void zincrby_new_key() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));

        // when
        zset.zincrby("key1", 5.0);

        // then
        then(zset.zscore("key1")).isEqualTo(5.0);
    }

    @Test
    @DisplayName("zadd로 직접 score를 설정할 수 있다")
    void zadd() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));

        // when
        zset.zadd("key1", 100.0);

        // then
        then(zset.zscore("key1")).isEqualTo(100.0);
    }

    @Test
    @DisplayName("zrange로 score 순으로 조회할 수 있다")
    void zrange() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        zset.zincrby("key1", 30.0);
        zset.zincrby("key2", 10.0);
        zset.zincrby("key3", 20.0);

        // when
        List<String> result = zset.zrange(0, -1);

        // then
        then(result).containsExactly("key2", "key3", "key1");
    }

    @Test
    @DisplayName("zrem으로 요소를 제거할 수 있다")
    void zrem() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        zset.zincrby("key1", 10.0);
        zset.zincrby("key2", 20.0);

        // when
        zset.zrem("key1");

        // then
        then(zset.zscore("key1")).isNull();
        then(zset.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("size로 현재 항목 수를 조회할 수 있다")
    void size() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        zset.zincrby("key1", 10.0);
        zset.zincrby("key2", 20.0);
        zset.zincrby("key3", 30.0);

        // when & then
        then(zset.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("TTL이 지나면 항목이 자동으로 만료된다")
    void ttl_expiration() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofMillis(100));
        zset.zincrby("key1", 10.0);
        zset.zincrby("key2", 20.0);

        // when
        await().atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(zset.zrange(0, -1)).isEmpty();
                then(zset.size()).isEqualTo(0);
            });
    }

    @Test
    @DisplayName("zscore 조회 시 만료된 항목은 null을 반환한다")
    void zscore_expired() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofMillis(100));
        zset.zincrby("key1", 10.0);

        // when
        await().atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(zset.zscore("key1")).isNull();
            });
    }

    @Test
    @DisplayName("zincrby 호출 시 TTL이 갱신된다")
    void ttl_refresh_on_zincrby() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofMillis(200));
        zset.zincrby("key1", 10.0);

        // when - 150ms 후 다시 zincrby (TTL 갱신)
        await().pollDelay(Duration.ofMillis(150))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> zset.zincrby("key1", 5.0));

        // then - 100ms 더 기다려도 아직 만료되지 않음 (총 250ms, TTL은 200ms 갱신됨)
        await().pollDelay(Duration.ofMillis(100))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(zset.zscore("key1")).isEqualTo(15.0);
            });
    }

    @Test
    @DisplayName("zadd 호출 시 TTL이 갱신된다")
    void ttl_refresh_on_zadd() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofMillis(200));
        zset.zadd("key1", 10.0);

        // when - 150ms 후 다시 zadd (TTL 갱신)
        await().pollDelay(Duration.ofMillis(150))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> zset.zadd("key1", 20.0));

        // then - 100ms 더 기다려도 아직 만료되지 않음
        await().pollDelay(Duration.ofMillis(100))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(zset.zscore("key1")).isEqualTo(20.0);
            });
    }

    @Test
    @DisplayName("일부 항목만 만료되고 나머지는 유지된다")
    void partial_expiration() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofMillis(200));
        zset.zincrby("key1", 10.0);

        // when - 100ms 후 key2 추가
        await().pollDelay(Duration.ofMillis(100))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> zset.zincrby("key2", 20.0));

        // then - 150ms 후 key1은 만료, key2는 유지
        await().pollDelay(Duration.ofMillis(150))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(zset.zrange(0, -1)).containsExactly("key2");
                then(zset.size()).isEqualTo(1);
            });
    }

    @Test
    @DisplayName("여러 key에 대해 zincrby를 혼합 사용할 수 있다")
    void multiple_keys_zincrby() {
        // given
        TtlZset<Long> zset = new TtlZset<>(Duration.ofHours(1));

        // when
        zset.zincrby(1L, 10.0);
        zset.zincrby(2L, 5.0);
        zset.zincrby(1L, 3.0);
        zset.zincrby(3L, 20.0);
        zset.zincrby(2L, 10.0);

        // then
        then(zset.zscore(1L)).isEqualTo(13.0);
        then(zset.zscore(2L)).isEqualTo(15.0);
        then(zset.zscore(3L)).isEqualTo(20.0);
        then(zset.zrange(0, -1)).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("zrem으로 제거하면 TTL도 함께 제거된다")
    void zrem_removes_ttl() {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofMillis(500));
        zset.zincrby("key1", 10.0);
        zset.zincrby("key2", 20.0);

        // when
        zset.zrem("key1");

        // then
        then(zset.size()).isEqualTo(1);
        then(zset.zscore("key1")).isNull();

        // key2는 여전히 존재
        await().pollDelay(Duration.ofMillis(200))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                then(zset.zscore("key2")).isEqualTo(20.0);
            });
    }
}
