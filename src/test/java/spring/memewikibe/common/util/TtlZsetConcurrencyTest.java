package spring.memewikibe.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;

class TtlZsetConcurrencyTest {

    @RepeatedTest(10)
    @DisplayName("여러 스레드가 동시에 zincrby를 호출하면 race condition이 발생할 수 있다")
    void concurrent_zincrby() throws InterruptedException {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 여러 스레드가 동시에 같은 key에 대해 zincrby 호출
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        zset.zincrby("key1", 1.0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then - 예상: threadCount * incrementsPerThread = 1000
        // 실제: race condition으로 인해 값이 누락될 수 있음
        Double score = zset.zscore("key1");

        // 이 assertion은 실패할 가능성이 높음 (동시성 이슈가 있다면)
        then(score).isEqualTo(1000.0);
    }

    @RepeatedTest(10)
    @DisplayName("여러 스레드가 동시에 서로 다른 key에 zincrby를 호출하면 안전해야 한다")
    void concurrent_zincrby_different_keys() throws InterruptedException {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 각 스레드가 다른 key에 대해 zincrby 호출
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        zset.zincrby("key" + threadId, 1.0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then - 각 key는 정확히 incrementsPerThread 만큼 증가되어야 함
        for (int i = 0; i < threadCount; i++) {
            Double score = zset.zscore("key" + i);
            then(score).isEqualTo(100.0);
        }
        then(zset.size()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("여러 스레드가 동시에 zadd, zrem, zincrby를 혼합 호출할 때 데이터 일관성이 깨질 수 있다")
    void concurrent_mixed_operations() throws InterruptedException {
        // given
        TtlZset<Integer> zset = new TtlZset<>(Duration.ofHours(1));
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 여러 스레드가 동시에 add, remove, increment 수행
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        int key = j % 10; // 10개의 key를 순환
                        switch (j % 3) {
                            case 0 -> zset.zadd(key, threadId * 10.0);
                            case 1 -> zset.zincrby(key, 1.0);
                            case 2 -> zset.zrem(key);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then - size()와 zrange()의 결과가 일치해야 함
        List<Integer> range = zset.zrange(0, -1);
        then(range.size()).isEqualTo(zset.size());
    }

    @RepeatedTest(10)
    @DisplayName("여러 스레드가 동시에 zrange를 호출하면서 zincrby도 호출하면 ConcurrentModificationException이 발생할 수 있다")
    void concurrent_read_write() throws InterruptedException {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofHours(1));
        for (int i = 0; i < 100; i++) {
            zset.zadd("key" + i, i * 1.0);
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = new ArrayList<>();

        // when - 절반은 read, 절반은 write
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    if (threadId % 2 == 0) {
                        // Read operations
                        for (int j = 0; j < 100; j++) {
                            zset.zrange(0, -1);
                        }
                    } else {
                        // Write operations
                        for (int j = 0; j < 100; j++) {
                            zset.zincrby("key" + (j % 100), 1.0);
                        }
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then - 예외가 발생하지 않아야 함
        then(exceptions).isEmpty();
    }

    @RepeatedTest(10)
    @DisplayName("TTL이 있는 상태에서 동시에 zincrby를 호출하면 TTL과 score가 불일치할 수 있다")
    void concurrent_zincrby_with_ttl() throws InterruptedException {
        // given
        TtlZset<String> zset = new TtlZset<>(Duration.ofSeconds(10));
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        zset.zincrby("key1", 1.0);
                        // zincrby와 TTL 갱신 사이에 다른 스레드가 개입할 수 있음
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then - score와 TTL이 모두 제대로 설정되어 있어야 함
        Double score = zset.zscore("key1");
        then(score).isNotNull();
        then(score).isEqualTo(1000.0);
        then(zset.size()).isEqualTo(1);
    }
}
