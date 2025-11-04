package spring.memewikibe.domain.meme;

import spring.memewikibe.common.util.TtlZset;

import java.util.List;

public class PopularCachedMeme {
    private final TtlZset<Long> cache;
    private final PopularMemeProperties properties;

    public PopularCachedMeme(PopularMemeProperties properties) {
        this.properties = properties;
        this.cache = new TtlZset<>(properties.ttl());
    }

    public void viewed(long memeId) {
        cache.zincrby(memeId, properties.viewScore());
    }

    public void shared(long memeId) {
        cache.zincrby(memeId, properties.shareScore());
    }

    public void customized(long memeId) {
        cache.zincrby(memeId, properties.customScore());
    }

    public List<Long> getTopMemes(int count) {
        return cache.zrange(0, count - 1);
    }

    public int size() {
        return cache.size();
    }

}
