package spring.memewikibe.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Redis의 Sorted Set(ZSET)을 모방한 자료구조
 *
 * 주의: 이 클래스는 thread-safe하지 않습니다.
 * 멀티스레드 환경에서 사용할 경우 외부에서 동기화가 필요합니다.
 * {@link TtlZset}은 이 클래스를 내부적으로 사용하며 동기화를 제공합니다.
 */
public class Zset<K> {
    private final Map<K, Double> dict = new HashMap<>();
    private final TreeSet<ScoreKey<K>> skip = new TreeSet<>();

    public void zadd(K key, double score) {
        Double old = dict.put(key, score);
        if (old != null) {
            skip.remove(new ScoreKey<>(old, key));
        }
        skip.add(new ScoreKey<>(score, key));
    }

    public void zincrby(K key, double increment) {
        Double current = dict.get(key);
        double newScore = (current != null ? current : 0.0) + increment;
        Double old = dict.put(key, newScore);
        if (old != null) {
            skip.remove(new ScoreKey<>(old, key));
        }
        skip.add(new ScoreKey<>(newScore, key));
    }

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

    public int size() {
        return skip.size();
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
