package spring.memewikibe.common.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * TTL(Time To Live)을 지원하는 Sorted Set
 *
 * 내부적으로 {@link Zset}을 사용하며, ReentrantReadWriteLock을 통해
 * thread-safe한 동작을 보장합니다.
 */
public class TtlZset<K> {
    private final Zset<K> zset = new Zset<>();
    private final Map<K, Long> ttl = new HashMap<>();
    private final Duration defaultTtl;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public TtlZset(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public void zincrby(K key, double increment) {
        lock.writeLock().lock();
        try {
            zset.zincrby(key, increment);
            ttl.put(key, System.currentTimeMillis() + defaultTtl.toMillis());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void zadd(K key, double score) {
        lock.writeLock().lock();
        try {
            zset.zadd(key, score);
            ttl.put(key, System.currentTimeMillis() + defaultTtl.toMillis());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void zrem(K key) {
        lock.writeLock().lock();
        try {
            zset.zrem(key);
            ttl.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Double zscore(K key) {
        lock.writeLock().lock();
        try {
            evictIfExpired(key);
            return zset.zscore(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<K> zrange(int start, int end) {
        lock.writeLock().lock();
        try {
            evictExpired();
            return zset.zrange(start, end);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<K> zrevrange(int start, int end) {
        lock.writeLock().lock();
        try {
            evictExpired();
            return zset.zrevrange(start, end);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.writeLock().lock();
        try {
            evictExpired();
            return ttl.size();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evictIfExpired(K key) {
        Long expiry = ttl.get(key);
        if (expiry != null && expiry < System.currentTimeMillis()) {
            zset.zrem(key);
            ttl.remove(key);
        }
    }

    // TODO: 성능 이슈 - O(n) 전체 스캔으로 항목이 많을 때 느려짐 아래로 개선 생각해볼 것
    //       1. Lazy Eviction: zrange 결과에서만 필터링
    //       2. Background Thread: 주기적으로 일부만 정리
    //       3. Sampling: Redis처럼 일부만 샘플링하여 체크
    private void evictExpired() {
        long now = System.currentTimeMillis();
        ttl.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                zset.zrem(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
