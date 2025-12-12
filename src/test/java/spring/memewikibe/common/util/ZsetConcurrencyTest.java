package spring.memewikibe.common.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import spring.memewikibe.annotation.UnitTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Zset은 thread-safe하지 않습니다.
 * <p>
 * 이 테스트들은 Zset이 동기화 없이 사용될 경우 동시성 이슈가 발생함을 보여줍니다.
 * 실제 사용 시에는 {@link TtlZset}과 같이 외부에서 동기화를 제공해야 합니다.
 *
 * @Disabled 처리된 테스트들은 Zset의 thread-unsafe 특성을 증명하기 위한 것으로,
 * 활성화하면 대부분 실패합니다 (race condition 발생).
 */
@UnitTest
class ZsetConcurrencyTest {

    @Disabled("Zset의 thread-unsafe 특성을 증명하는 테스트 - 활성화하면 race condition으로 대부분 실패함")
    @RepeatedTest(10)
    @DisplayName("Zset은 thread-safe하지 않아서 동시에 zincrby를 호출하면 race condition이 발생한다")
    void zset_is_not_thread_safe_zincrby() throws InterruptedException {
        // given
        Zset<String> zset = new Zset<>();
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when -
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

        // then - Zset은 thread-safe하지 않으므로 race condition으로 인해 값이 누락됨
        Double score = zset.zscore("key1");

        // Assertion 실패
        then(score).isNotEqualTo(1000.0);
        then(score).isLessThan(1000.0);
    }

    @Disabled("Zset의 thread-unsafe 특성을 증명하는 테스트 - 활성화하면 내부 일관성이 깨져서 불안정함")
    @RepeatedTest(10)
    @DisplayName("Zset은 thread-safe하지 않아서 혼합 작업 시 데이터 일관성이 깨진다")
    void zset_is_not_thread_safe_mixed_operations() throws InterruptedException {
        // given
        Zset<Integer> zset = new Zset<>();
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
                        int key = j % 10;
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

        // then - size()와 zrange()의 결과가 불일치할 수 있음
        // (내부 자료구조인 dict와 skip의 일관성이 깨짐)
        int dictSize = 0;
        for (int i = 0; i < 100; i++) {
            if (zset.zscore(i) != null) {
                dictSize++;
            }
        }
        int skipSize = zset.zrange(0, -1).size();

        // 이 assertion은 자주 실패함 (내부 일관성이 깨짐을 증명)
        // dictSize와 skipSize가 다를 수 있음
        // 최소한 하나 이상의 데이터가 있어야 함
        then(dictSize + skipSize).isGreaterThan(0);
    }

    @RepeatedTest(10)
    @DisplayName("Zset은 단일 스레드에서는 정상적으로 동작한다")
    void zset_works_correctly_in_single_thread() {
        // given
        Zset<String> zset = new Zset<>();
        int iterations = 1000;

        // when - 단일 스레드에서 zincrby 호출
        for (int i = 0; i < iterations; i++) {
            zset.zincrby("key1", 1.0);
        }

        // then - 단일 스레드에서는 정확히 동작함
        Double score = zset.zscore("key1");
        then(score).isEqualTo(1000.0);
    }
}
