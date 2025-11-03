package spring.memewikibe.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TopKScoredCacheTest {

    @Test
    @DisplayName("기본 incrementScore와 getTopK 동작 확인")
    void basicIncrementAndGet() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 20.0, "data2");
        cache.incrementScore("meme3", 5.0, "data3");

        // when
        List<TopKScoredCache.CacheEntry<String, String>> topK = cache.getTopK();

        // then
        assertThat(topK).hasSize(3);
        assertThat(topK.get(0).getKey()).isEqualTo("meme2");  // score 20
        assertThat(topK.get(0).getScore()).isEqualTo(20.0);
        assertThat(topK.get(1).getKey()).isEqualTo("meme1");  // score 10
        assertThat(topK.get(2).getKey()).isEqualTo("meme3");  // score 5
    }

    @Test
    @DisplayName("Top-K 경계: K개를 초과하면 상위 K개만 반환")
    void topKBoundary() {
        // given - 10개 추가 (K=6)
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        for (int i = 1; i <= 10; i++) {
            cache.incrementScore("meme" + i, i * 10.0, "data" + i);
        }

        // when
        List<TopKScoredCache.CacheEntry<String, String>> topK = cache.getTopK();

        // then - 상위 6개만 반환 (100, 90, 80, 70, 60, 50)
        assertThat(topK).hasSize(6);
        assertThat(topK.get(0).getScore()).isEqualTo(100.0);
        assertThat(topK.get(5).getScore()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Score 업데이트 시 재정렬")
    void scoreUpdateReordering() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 20.0, "data2");
        cache.incrementScore("meme3", 5.0, "data3");

        // when - meme3의 score를 증가시켜 1위로
        cache.incrementScore("meme3", 20.0, null);  // 5 + 20 = 25

        // then
        List<TopKScoredCache.CacheEntry<String, String>> topK = cache.getTopK();
        assertThat(topK.get(0).getKey()).isEqualTo("meme3");  // score 25
        assertThat(topK.get(0).getScore()).isEqualTo(25.0);
        assertThat(topK.get(1).getKey()).isEqualTo("meme2");  // score 20
    }

    @Test
    @DisplayName("TTL 만료 후 getTopK에서 제외")
    void ttlExpiration() throws InterruptedException {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 20.0, "data2");

        // when - TTL(1초) 대기
        Thread.sleep(1100);

        // then - 만료된 항목은 조회되지 않음
        List<TopKScoredCache.CacheEntry<String, String>> topK = cache.getTopK();
        assertThat(topK).isEmpty();
    }

    @Test
    @DisplayName("getScore로 특정 키의 점수 조회")
    void getScore() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");

        // when
        Double score = cache.getScore("meme1");

        // then
        assertThat(score).isEqualTo(10.0);
    }

    @Test
    @DisplayName("존재하지 않는 키의 score는 null")
    void getScore_notFound() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        // when
        Double score = cache.getScore("nonexistent");

        // then
        assertThat(score).isNull();
    }

    @Test
    @DisplayName("get으로 특정 키의 값 조회")
    void getValue() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");

        // when
        String value = cache.get("meme1");

        // then
        assertThat(value).isEqualTo("data1");
    }

    @Test
    @DisplayName("valueLoader 동작 확인 - value가 null이면 loader 사용")
    void valueLoaderUsage() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, null);

        // when
        String value = cache.get("meme1");

        // then - valueLoader가 "value:" + key를 반환
        assertThat(value).isEqualTo("value:meme1");
    }

    @Test
    @DisplayName("evictExpired로 만료된 항목 제거")
    void evictExpired() throws InterruptedException {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 20.0, "data2");

        // when - TTL 만료 후 evict 실행
        Thread.sleep(1100);
        int evictedCount = cache.evictExpired();

        // then
        assertThat(evictedCount).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Lazy deletion: TTL 갱신 시 heap에 중복 엔트리 생성, 버전으로 필터링")
    void lazyDeletion() throws InterruptedException {
        // given - 같은 키를 여러 번 업데이트
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        Thread.sleep(100);
        cache.incrementScore("meme1", 5.0, null);   // score 15, 새 version
        Thread.sleep(100);
        cache.incrementScore("meme1", 10.0, null);  // score 25, 새 version

        // when - 첫 번째 TTL이 만료되어도 최신 version은 유지
        Thread.sleep(900);
        int evictedBefore = cache.evictExpired();

        // then - stale 엔트리는 무시되고, 유효한 데이터는 남아있음
        assertThat(evictedBefore).isEqualTo(0);
        assertThat(cache.getScore("meme1")).isEqualTo(25.0);

        // 모든 TTL 만료 후
        Thread.sleep(200);
        int evictedAfter = cache.evictExpired();
        assertThat(evictedAfter).isEqualTo(1);
    }

    @Test
    @DisplayName("cleanupStaleScoreIndex로 scoreIndex의 stale 엔트리 제거")
    void cleanupStaleScoreIndex() throws InterruptedException {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 20.0, "data2");

        // when - TTL 만료 후 scoreIndex cleanup
        Thread.sleep(1100);
        int removedCount = cache.cleanupStaleScoreIndex();

        // then - scoreIndex에서 제거됨
        assertThat(removedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("동시성 테스트: 여러 스레드가 동시에 incrementScore")
    void concurrentIncrementScore() throws InterruptedException {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 10개 스레드가 각각 100번씩 increment
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        cache.incrementScore("meme1", 1.0, "data");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then - 총 1000번 증가
        Double finalScore = cache.getScore("meme1");
        assertThat(finalScore).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("동시성 테스트: incrementScore와 getTopK 동시 실행")
    void concurrentReadWrite() throws InterruptedException {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        int writerCount = 5;
        int readerCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
        CountDownLatch latch = new CountDownLatch(writerCount + readerCount);

        // when - writer와 reader 동시 실행
        for (int i = 0; i < writerCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        cache.incrementScore("meme" + threadId, 1.0, "data");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        cache.getTopK();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then - 데이터 무결성 확인
        List<TopKScoredCache.CacheEntry<String, String>> topK = cache.getTopK();
        assertThat(topK).hasSize(5);  // meme0 ~ meme4
        for (var entry : topK) {
            assertThat(entry.getScore()).isEqualTo(100.0);
        }
    }

    @Test
    @DisplayName("clear로 모든 항목 제거")
    void clearCache() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 20.0, "data2");

        // when
        cache.clear();

        // then
        assertThat(cache.size()).isEqualTo(0);
        assertThat(cache.getTopK()).isEmpty();
    }

    @Test
    @DisplayName("score 0에서 시작하여 증가")
    void incrementFromZero() {
        // given - 처음부터 없는 키
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("newMeme", 5.0, "data");

        // when
        Double score = cache.getScore("newMeme");

        // then - 0 + 5 = 5
        assertThat(score).isEqualTo(5.0);
    }

    @Test
    @DisplayName("음수 delta로 score 감소")
    void negativeScoreDelta() {
        // given
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 100.0, "data1");

        // when - 음수 delta로 감소
        cache.incrementScore("meme1", -30.0, null);

        // then
        assertThat(cache.getScore("meme1")).isEqualTo(70.0);
    }

    @Test
    @DisplayName("같은 score일 때 키로 정렬 (deterministic)")
    void tieBreakingByKey() {
        // given - 같은 score
        TopKScoredCache<String, String> cache = new TopKScoredCache<>(6, 1000L, key -> "value:" + key);

        cache.incrementScore("meme1", 10.0, "data1");
        cache.incrementScore("meme2", 10.0, "data2");
        cache.incrementScore("meme3", 10.0, "data3");

        // when
        List<TopKScoredCache.CacheEntry<String, String>> topK = cache.getTopK();

        // then - 모두 같은 score지만 deterministic한 순서여야 함
        assertThat(topK).hasSize(3);
        assertThat(topK).allMatch(entry -> entry.getScore() == 10.0);

        // 같은 score일 때 키로 정렬되며, descendingMap() 사용으로 역순
        List<String> keys = topK.stream()
            .map(TopKScoredCache.CacheEntry::getKey)
            .toList();
        assertThat(keys).containsExactly("meme3", "meme2", "meme1");
    }
}
