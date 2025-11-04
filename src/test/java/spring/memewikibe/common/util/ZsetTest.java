package spring.memewikibe.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZsetTest {

    @Test
    @DisplayName("zadd로 요소를 추가하고 zscore로 조회할 수 있다")
    void zadd_and_zscore() {
        // given
        Zset<String> zset = new Zset<>();

        // when
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);
        zset.zadd("key3", 15.0);

        // then
        assertThat(zset.zscore("key1")).isEqualTo(10.0);
        assertThat(zset.zscore("key2")).isEqualTo(20.0);
        assertThat(zset.zscore("key3")).isEqualTo(15.0);
    }

    @Test
    @DisplayName("zadd로 같은 key에 새로운 score를 설정하면 덮어쓴다")
    void zadd_overwrite() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);

        // when
        zset.zadd("key1", 30.0);

        // then
        assertThat(zset.zscore("key1")).isEqualTo(30.0);
    }

    @Test
    @DisplayName("zrange로 score 오름차순으로 조회할 수 있다")
    void zrange_ascending_order() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 30.0);
        zset.zadd("key2", 10.0);
        zset.zadd("key3", 20.0);
        zset.zadd("key4", 40.0);
        zset.zadd("key5", 5.0);

        // when
        List<String> result = zset.zrange(0, 4);

        // then
        assertThat(result).containsExactly("key5", "key2", "key3", "key1", "key4");
    }

    @Test
    @DisplayName("zrange로 부분 범위를 조회할 수 있다")
    void zrange_partial() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);
        zset.zadd("key3", 30.0);
        zset.zadd("key4", 40.0);
        zset.zadd("key5", 50.0);

        // when
        List<String> result = zset.zrange(1, 3);

        // then
        assertThat(result).containsExactly("key2", "key3", "key4");
    }

    @Test
    @DisplayName("zrange는 음수 인덱스를 지원한다 (Redis 스타일)")
    void zrange_negative_index() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);
        zset.zadd("key3", 30.0);
        zset.zadd("key4", 40.0);
        zset.zadd("key5", 50.0);

        // when & then
        // 전체 조회
        assertThat(zset.zrange(0, -1)).containsExactly("key1", "key2", "key3", "key4", "key5");

        // 마지막 3개
        assertThat(zset.zrange(-3, -1)).containsExactly("key3", "key4", "key5");

        // 마지막 2개
        assertThat(zset.zrange(-2, -1)).containsExactly("key4", "key5");

        // 처음부터 마지막 직전까지
        assertThat(zset.zrange(0, -2)).containsExactly("key1", "key2", "key3", "key4");
    }

    @Test
    @DisplayName("zrange에서 start > end면 빈 리스트를 반환한다")
    void zrange_invalid_range() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);

        // when
        List<String> result = zset.zrange(5, 3);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("zrange에서 범위가 size를 넘어가면 정규화된다")
    void zrange_out_of_bounds() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);

        // when
        List<String> result = zset.zrange(0, 100);

        // then
        assertThat(result).containsExactly("key1", "key2");
    }

    @Test
    @DisplayName("zrem으로 요소를 제거할 수 있다")
    void zrem() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);
        zset.zadd("key3", 30.0);

        // when
        zset.zrem("key2");

        // then
        assertThat(zset.zscore("key2")).isNull();
        assertThat(zset.zrange(0, -1)).containsExactly("key1", "key3");
    }

    @Test
    @DisplayName("존재하지 않는 key를 zrem해도 에러가 발생하지 않는다")
    void zrem_non_existent() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);

        // when & then
        zset.zrem("non_existent");
        assertThat(zset.zrange(0, -1)).containsExactly("key1");
    }

    @Test
    @DisplayName("같은 score를 가진 요소들은 key로 정렬된다 (Long 타입)")
    void same_score_ordering_long() {
        // given
        Zset<Long> zset = new Zset<>();
        zset.zadd(3L, 10.0);
        zset.zadd(1L, 10.0);
        zset.zadd(2L, 10.0);

        // when
        List<Long> result = zset.zrange(0, -1);

        // then
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("같은 score를 가진 요소들은 key로 정렬된다 (String 타입)")
    void same_score_ordering_string() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("charlie", 10.0);
        zset.zadd("alice", 10.0);
        zset.zadd("bob", 10.0);

        // when
        List<String> result = zset.zrange(0, -1);

        // then
        assertThat(result).containsExactly("alice", "bob", "charlie");
    }

    @Test
    @DisplayName("음수 score도 지원한다")
    void negative_score() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", -10.0);
        zset.zadd("key2", 0.0);
        zset.zadd("key3", 10.0);

        // when
        List<String> result = zset.zrange(0, -1);

        // then
        assertThat(result).containsExactly("key1", "key2", "key3");
    }

    @Test
    @DisplayName("빈 zset에서 zrange를 호출하면 빈 리스트를 반환한다")
    void empty_zset() {
        // given
        Zset<String> zset = new Zset<>();

        // when
        List<String> result = zset.zrange(0, -1);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("zadd로 score 업데이트 시 정렬 순서가 변경된다")
    void zadd_update_order() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);
        zset.zadd("key2", 20.0);
        zset.zadd("key3", 30.0);

        // when
        zset.zadd("key1", 40.0);  // 10 → 40으로 변경

        // then
        List<String> result = zset.zrange(0, -1);
        assertThat(result).containsExactly("key2", "key3", "key1");
    }

    @Test
    @DisplayName("zincrby로 score를 증가시킬 수 있다")
    void zincrby() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);

        // when
        zset.zincrby("key1", 5.0);
        zset.zincrby("key1", 3.0);

        // then
        assertThat(zset.zscore("key1")).isEqualTo(18.0);
    }

    @Test
    @DisplayName("zincrby는 존재하지 않는 key에 대해 0에서 시작한다")
    void zincrby_new_key() {
        // given
        Zset<String> zset = new Zset<>();

        // when
        zset.zincrby("key1", 5.0);

        // then
        assertThat(zset.zscore("key1")).isEqualTo(5.0);
    }

    @Test
    @DisplayName("zincrby로 음수 값을 증가시킬 수 있다 (감소)")
    void zincrby_negative() {
        // given
        Zset<String> zset = new Zset<>();
        zset.zadd("key1", 10.0);

        // when
        zset.zincrby("key1", -3.0);

        // then
        assertThat(zset.zscore("key1")).isEqualTo(7.0);
    }
}
