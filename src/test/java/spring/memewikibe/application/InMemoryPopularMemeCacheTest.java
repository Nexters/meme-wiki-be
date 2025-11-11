package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@UnitTest
class InMemoryPopularMemeCacheTest {

    private InMemoryPopularMemeCache cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryPopularMemeCache();
    }

    @Test
    void 초기_캐시는_비어있다() {
        // when
        List<Long> result = cache.getTopPopularMemeIds();

        // then
        then(result).isEmpty();
    }

    @Test
    void onMemeViewed는_밈의_점수를_증가시킨다() {
        // when
        cache.onMemeViewed(1L);
        List<Long> result = cache.getTopPopularMemeIds();

        // then
        then(result).hasSize(1);
        then(result.get(0)).isEqualTo(1L);
    }

    @Test
    void onMemeCustomized는_밈의_점수를_view보다_높게_증가시킨다() {
        // when
        cache.onMemeViewed(1L);       // 1점
        cache.onMemeCustomized(2L);   // 2점
        List<Long> result = cache.getTopPopularMemeIds();

        // then - custom이 view보다 점수가 높아서 먼저 나옴
        then(result).hasSize(2);
        then(result.get(0)).isEqualTo(2L);  // 2점 (score 내림차순)
        then(result.get(1)).isEqualTo(1L);  // 1점
    }

    @Test
    void onMemeShared는_밈의_점수를_가장_높게_증가시킨다() {
        // when
        cache.onMemeViewed(1L);       // 1점
        cache.onMemeCustomized(2L);   // 2점
        cache.onMemeShared(3L);       // 3점
        List<Long> result = cache.getTopPopularMemeIds();

        // then - score 오름차순
        then(result).hasSize(3);
        then(result.get(0)).isEqualTo(3L);  // share (3점)
        then(result.get(1)).isEqualTo(2L);  // custom (2점)
        then(result.get(2)).isEqualTo(1L);  // view (1점)
    }

    @Test
    void 동일한_밈에_여러_이벤트가_발생하면_점수가_누적된다() {
        // when
        cache.onMemeViewed(1L);       // +1 = 1
        cache.onMemeViewed(1L);       // +1 = 2
        cache.onMemeCustomized(1L);   // +2 = 4
        cache.onMemeShared(1L);       // +3 = 7
        List<Long> result = cache.getTopPopularMemeIds();

        // then
        then(result).hasSize(1);
        then(result.get(0)).isEqualTo(1L);
    }

    @Test
    void Top6를_초과하는_밈은_상위_6개만_반환된다() {
        // given - 10개 밈 추가 (점수: 1, 2, 3, ..., 10)
        for (int i = 1; i <= 10; i++) {
            for (int j = 0; j < i; j++) {
                cache.onMemeViewed((long) i);
            }
        }

        // when - TOP_K = 6 제한으로 상위 6개만 가져옴
        List<Long> result = cache.getTopPopularMemeIds();

        // then - Top 6만 반환됨 (zrange는 score 오름차순이므로 낮은 점수부터)
        then(result).hasSize(6);
        // 점수: 1L=1점, 2L=2점, ..., 10L=10점
        // zrange(0, 5)는 처음 6개 = 점수가 낮은 순 1,2,3,4,5,6
        then(result).containsExactly(10L, 9L, 8L, 7L, 6L, 5L);
    }

    @Test
    void initializeWithMemeIds는_여러_밈ID로_캐시를_초기화한다() {
        // given
        List<Long> memeIds = List.of(1L, 2L, 3L);

        // when
        cache.initializeWithMemeIds(memeIds);
        List<Long> result = cache.getTopPopularMemeIds();

        // then
        then(result).hasSize(3);
        then(result).containsExactlyInAnyOrderElementsOf(memeIds);
    }

    @Test
    void 같은_밈을_다시_보면_점수가_누적된다() {
        // given
        cache.onMemeViewed(1L);
        cache.onMemeViewed(2L);

        // when - meme2를 한 번 더 봄
        cache.onMemeViewed(2L);
        List<Long> result = cache.getTopPopularMemeIds();

        // then - meme2가 더 높은 점수로 상위에 위치
        then(result).hasSize(2);
        then(result.get(0)).isEqualTo(2L);  // 2점
        then(result.get(1)).isEqualTo(1L);  // 1점
    }

    @Test
    void getTargetSize는_목표_크기를_반환한다() {
        // when & then
        then(cache.getTargetSize()).isEqualTo(6);
    }
}
