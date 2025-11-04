package spring.memewikibe.common.util;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TtlZset<K> {
    private final Zset<K> zset = new Zset<>();
    private final ConcurrentHashMap<K, Long> ttl = new ConcurrentHashMap<>();
    private final Duration defaultTtl;

    public TtlZset(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    // TODO: 동시성 이슈 - zincrby와 ttl.put 사이에 원자성이 보장되지 않음
    public void zincrby(K key, double increment) {
        zset.zincrby(key, increment);
        ttl.put(key, System.currentTimeMillis() + defaultTtl.toMillis());
    }

    public void zadd(K key, double score) {
        zset.zadd(key, score);
        ttl.put(key, System.currentTimeMillis() + defaultTtl.toMillis());
    }

    public void zrem(K key) {
        zset.zrem(key);
        ttl.remove(key);
    }

    public Double zscore(K key) {
        evictIfExpired(key);
        return zset.zscore(key);
    }

    public List<K> zrange(int start, int end) {
        evictExpired();
        return zset.zrange(start, end);
    }

    public int size() {
        evictExpired();
        return ttl.size();
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
