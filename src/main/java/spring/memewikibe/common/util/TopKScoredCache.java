package spring.memewikibe.common.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * TTL을 지원하는 Thread-safe 점수 기반 캐시.
 * Redis ZSET과 유사하며 자동 만료 기능을 제공함.
 * Heap에서 O(n) 제거를 피하기 위해 Lazy Deletion 패턴을 사용함.
 *
 * @param <K> 키 타입
 * @param <V> 값 타입
 */
@Slf4j
public class TopKScoredCache<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<K, V>> dataMap;
    private final ConcurrentSkipListMap<ScoredKey<K>, K> scoreIndex;
    private final PriorityBlockingQueue<ExpiryEntry<K>> expiryHeap;
    private final AtomicLong versionCounter;

    private final int k;
    private final long ttlMillis;
    private final Function<K, V> valueLoader;

    /**
     * @param k 상위 K개 항목을 유지
     * @param ttlMillis TTL (밀리초 단위)
     * @param valueLoader 키가 없을 때 값을 로드하는 함수
     */
    public TopKScoredCache(int k, long ttlMillis, Function<K, V> valueLoader) {
        this.k = k;
        this.ttlMillis = ttlMillis;
        this.valueLoader = valueLoader;
        this.dataMap = new ConcurrentHashMap<>();
        this.scoreIndex = new ConcurrentSkipListMap<>();
        this.expiryHeap = new PriorityBlockingQueue<>();
        this.versionCounter = new AtomicLong(0);
    }

    /**
     * 점수를 delta만큼 증감 (Redis ZINCRBY와 유사).
     * TTL을 자동으로 갱신함.
     *
     * @param key 증감할 키
     * @param delta 더할 점수 (음수 가능)
     * @param value 저장할 값 (null이면 valueLoader 사용)
     */
    public void incrementScore(K key, double delta, V value) {
        long now = System.currentTimeMillis();

        dataMap.compute(key, (k, oldEntry) -> {
            double newScore = (oldEntry != null ? oldEntry.score : 0.0) + delta;
            long newExpiry = now + ttlMillis;
            long newVersion = versionCounter.incrementAndGet();

            // 기존 점수를 인덱스에서 제거
            if (oldEntry != null) {
                scoreIndex.remove(new ScoredKey<>(oldEntry.score, k));
            }

            // 새 점수를 인덱스에 추가
            scoreIndex.put(new ScoredKey<>(newScore, k), k);

            // 만료 힙에 추가 (lazy deletion - 중복 허용)
            expiryHeap.offer(new ExpiryEntry<>(k, newExpiry, newVersion));

            V finalValue = value != null ? value :
                          (oldEntry != null ? oldEntry.value : valueLoader.apply(k));

            return new CacheEntry<>(k, finalValue, newScore, newExpiry, newVersion);
        });
    }

    /**
     * 점수 기준 상위 K개 항목 조회 (내림차순).
     * 만료된 항목은 lazy deletion으로 건너뜀.
     *
     * @return 상위 K개 엔트리 리스트
     */
    public List<CacheEntry<K, V>> getTopK() {
        long now = System.currentTimeMillis();
        List<CacheEntry<K, V>> result = new ArrayList<>(k);

        // 높은 점수부터 낮은 점수 순으로 순회
        for (K key : scoreIndex.descendingMap().values()) {
            CacheEntry<K, V> entry = dataMap.get(key);

            // Lazy deletion: 만료되었거나 제거된 항목은 건너뜀
            if (entry == null || entry.expiryTime <= now) {
                continue;
            }

            result.add(entry);

            if (result.size() >= k) {
                break;
            }
        }

        return result;
    }

    /**
     * 특정 키의 점수 조회.
     *
     * @param key 조회할 키
     * @return 현재 점수, 없거나 만료되면 null
     */
    public Double getScore(K key) {
        CacheEntry<K, V> entry = dataMap.get(key);
        if (entry == null || entry.expiryTime <= System.currentTimeMillis()) {
            return null;
        }
        return entry.score;
    }

    /**
     * 특정 키의 값 조회.
     *
     * @param key 조회할 키
     * @return 현재 값, 없거나 만료되면 null
     */
    public V get(K key) {
        CacheEntry<K, V> entry = dataMap.get(key);
        if (entry == null || entry.expiryTime <= System.currentTimeMillis()) {
            return null;
        }
        return entry.value;
    }

    /**
     * 만료된 항목을 캐시에서 제거.
     * 백그라운드 스레드에서 주기적으로 호출해야 함.
     *
     * @return 제거된 항목 수
     */
    public int evictExpired() {
        long now = System.currentTimeMillis();
        int evictedCount = 0;

        // 힙에서 만료된 항목 처리
        while (!expiryHeap.isEmpty()) {
            ExpiryEntry<K> expiryEntry = expiryHeap.peek();

            // 힙은 만료순으로 정렬되어있으므로 현재 꺼낸 것이 만료되지 않았다면 break
            if (expiryEntry.expiryTime > now) {
                break;
            }

            expiryHeap.poll();

            // Lazy deletion: version이 일치하는 경우만 제거 (유효한 만료)
            CacheEntry<K, V> removed = dataMap.computeIfPresent(expiryEntry.key, (k, cacheEntry) -> {
                if (cacheEntry.version == expiryEntry.version) {
                    // 유효한 만료 - score 인덱스와 데이터 맵에서 제거
                    scoreIndex.remove(new ScoredKey<>(cacheEntry.score, k));
                    return null;  // 맵에서 제거
                }
                // Stale 만료 엔트리 - 현재 데이터 유지
                return cacheEntry;
            });

            if (removed == null) {
                evictedCount++;
            }
        }

        if (evictedCount > 0) {
            log.debug("Evicted {} expired entries", evictedCount);
        }

        return evictedCount;
    }

    /**
     * Score 인덱스의 stale 엔트리 정리.
     * 메모리 누수 방지를 위해 주기적으로 호출해야 함.
     *
     * @return 제거된 stale 엔트리 수
     */
    public int cleanupStaleScoreIndex() {
        int removedCount = 0;
        long now = System.currentTimeMillis();

        var iterator = scoreIndex.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            K key = entry.getValue();
            CacheEntry<K, V> cached = dataMap.get(key);

            // 데이터 맵에 없거나, 만료되었거나, 점수가 불일치하면 제거
            if (cached == null ||
                cached.expiryTime <= now ||
                Double.compare(cached.score, entry.getKey().score) != 0) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned up {} stale score index entries", removedCount);
        }

        return removedCount;
    }

    public int size() {
        return dataMap.size();
    }

    public void clear() {
        dataMap.clear();
        scoreIndex.clear();
        expiryHeap.clear();
    }

    @Getter
    public static class CacheEntry<K, V> {
        private final K key;
        private final V value;
        private final double score;
        private final long expiryTime;
        private final long version;

        public CacheEntry(K key, V value, double score, long expiryTime, long version) {
            this.key = key;
            this.value = value;
            this.score = score;
            this.expiryTime = expiryTime;
            this.version = version;
        }
    }

    private static class ScoredKey<K> implements Comparable<ScoredKey<K>> {
        private final double score;
        private final K key;

        public ScoredKey(double score, K key) {
            this.score = score;
            this.key = key;
        }

        @Override
        public int compareTo(ScoredKey<K> other) {
            // 1차: 점수 비교
            int scoreCompare = Double.compare(this.score, other.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }

            // 2차: 키로 비교 (deterministic한 순서 보장)
            if (this.key instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<K> comparableKey = (Comparable<K>) this.key;
                return comparableKey.compareTo(other.key);
            }

            // Fallback: hashCode로 비교
            return Integer.compare(this.key.hashCode(), other.key.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScoredKey<?> scoredKey = (ScoredKey<?>) o;
            return Double.compare(scoredKey.score, score) == 0 &&
                   Objects.equals(key, scoredKey.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(score, key);
        }
    }

    private static class ExpiryEntry<K> implements Comparable<ExpiryEntry<K>> {
        private final K key;
        private final long expiryTime;
        private final long version;

        public ExpiryEntry(K key, long expiryTime, long version) {
            this.key = key;
            this.expiryTime = expiryTime;
            this.version = version;
        }

        @Override
        public int compareTo(ExpiryEntry<K> other) {
            return Long.compare(this.expiryTime, other.expiryTime);
        }
    }
}
