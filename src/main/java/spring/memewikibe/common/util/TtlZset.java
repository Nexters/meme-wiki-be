package spring.memewikibe.common.util;

import java.time.Duration;
import java.util.ArrayList;
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
            // Verify consistency between ttl and zset
            int ttlSize = ttl.size();
            int zsetSize = zset.size();
            if (ttlSize != zsetSize) {
                throw new IllegalStateException(
                    String.format("Inconsistent state: ttl.size()=%d, zset.size()=%d", ttlSize, zsetSize)
                );
            }
            return ttlSize;
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

    private void evictExpired() {
        long now = System.currentTimeMillis();
        int sampleSize = 20;
        double expiredThreshold = 0.25; // 25%
        int maxIterations = 10;

        if (ttl.isEmpty()) {
            return;
        }

        int iteration = 0;
        while (iteration++ < maxIterations) {
            List<K> sample = new ArrayList<>();
            int count = 0;
            for (K key : ttl.keySet()) {
                sample.add(key);
                if (++count >= Math.min(sampleSize, ttl.size())) {
                    break;
                }
            }

            if (sample.isEmpty()) {
                break;
            }

            int expiredCount = 0;
            for (K key : sample) {
                Long expiry = ttl.get(key);
                if (expiry != null && expiry < now) {
                    zset.zrem(key);
                    ttl.remove(key);
                    expiredCount++;
                }
            }

            double expiredRate = (double) expiredCount / sample.size();
            if (expiredRate < expiredThreshold) {
                break;
            }
        }
    }
}
