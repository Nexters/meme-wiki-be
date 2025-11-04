package spring.memewikibe.domain.meme;

import spring.memewikibe.common.util.TtlZset;

import java.util.List;

public class PopularMemeRanking {
    private final TtlZset<Long> cache;
    private final PopularMemeRankingConfig config;

    public PopularMemeRanking(PopularMemeRankingConfig config) {
        this.config = config;
        this.cache = new TtlZset<>(config.ttl());
    }

    public void viewed(long memeId) {
        cache.zincrby(memeId, config.viewScore());
    }

    public void shared(long memeId) {
        cache.zincrby(memeId, config.shareScore());
    }

    public void customized(long memeId) {
        cache.zincrby(memeId, config.customScore());
    }

    public List<Long> getTopMemes(int count) {
        return cache.zrevrange(0, count - 1);
    }

    public int size() {
        return cache.size();
    }

}
