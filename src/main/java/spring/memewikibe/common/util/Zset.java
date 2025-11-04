package spring.memewikibe.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Zset<K> {
    private final Map<K, Double> dict = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<ScoreKey<K>> skip = new ConcurrentSkipListSet<>();

    // TODO: 동시성 이슈 - dict.put과 skip.remove/add 사이에 원자성이 보장되지 않음
    public void zadd(K key, double score) {
        Double old = dict.put(key, score);
        if (old != null) {
            skip.remove(new ScoreKey<>(old, key));
        }
        skip.add(new ScoreKey<>(score, key));
    }

    // TODO: 동시성 이슈 - read-modify-write race condition 발생 가능
    public void zincrby(K key, double increment) {
        Double current = dict.get(key);
        double newScore = (current != null ? current : 0.0) + increment;
        zadd(key, newScore);
    }

    // TODO: 동시성 이슈 - dict.remove와 skip.remove 사이에 원자성이 보장되지 않음
    public void zrem(K key) {
        Double score = dict.remove(key);
        if (score != null) {
            skip.remove(new ScoreKey<>(score, key));
        }
    }

    public Double zscore(K key) {
        return dict.get(key);
    }

    public List<K> zrange(int start, int end) {
        int size = skip.size();

        if (start < 0) start = size + start;
        if (end < 0) end = size + end;

        if (start < 0) start = 0;
        if (end >= size) end = size - 1;

        if (start > end) return List.of();

        ArrayList<K> result = new ArrayList<>();
        int idx = 0;
        for (ScoreKey<K> key : skip) {
            if (idx > end) break;
            if (idx >= start) {
                result.add(key.key);
            }
            idx++;
        }
        return result;
    }

    public List<K> zrevrange(int start, int end) {
        int size = skip.size();

        if (start < 0) start = size + start;
        if (end < 0) end = size + end;

        if (start < 0) start = 0;
        if (end >= size) end = size - 1;

        if (start > end) return List.of();

        ArrayList<K> result = new ArrayList<>();
        int idx = 0;
        for (ScoreKey<K> key : skip.descendingSet()) {
            if (idx > end) break;
            if (idx >= start) {
                result.add(key.key);
            }
            idx++;
        }
        return result;
    }

    private record ScoreKey<K>(double score, K key) implements Comparable<ScoreKey<K>> {

        @Override
        public int compareTo(ScoreKey<K> other) {
            int c = Double.compare(this.score, other.score);
            if (c != 0) return c;

            if (key instanceof Comparable<?> cmp1 && other.key instanceof Comparable<?> cmp2) {
                @SuppressWarnings("unchecked")
                int compare = ((Comparable<Object>) cmp1).compareTo(other.key);
                return compare;
            }
            return Integer.compare(System.identityHashCode(key), System.identityHashCode(other.key));
        }
    }

}
