package spring.memewikibe.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.common.util.TopKScoredCache;

import java.time.Duration;
import java.util.List;

/**
 * 인기 밈을 위한 순수 캐시 컴포넌트.
 * 실시간 점수 기반 Top K 캐싱을 제공하며, 비즈니스 로직은 포함하지 않음.
 */
@Slf4j
@Component
public class PopularMemeCache {

    private static final int TOP_K = 6;
    private static final Duration TTL = Duration.ofDays(7);

    private static final double VIEW_SCORE = 1.0;
    private static final double CUSTOM_SCORE = 2.0;  // 커스텀은 조회보다 2배 가중
    private static final double SHARE_SCORE = 3.0;  // 공유는 조회보다 3배 가중

    private final TopKScoredCache<Long, MemeSimpleResponse> cache;

    public PopularMemeCache() {
        this.cache = new TopKScoredCache<>(
            TOP_K,
            TTL.toMillis(),
            memeId -> null
        );
    }

    public void onMemeViewed(Long memeId, MemeSimpleResponse meme) {
        cache.incrementScore(memeId, VIEW_SCORE, meme);
        log.debug("Meme viewed: id={}, score increased by {}", memeId, VIEW_SCORE);
    }

    public void onMemeCustomized(Long memeId, MemeSimpleResponse meme) {
        cache.incrementScore(memeId, CUSTOM_SCORE, meme);
        log.debug("Meme customized: id={}, score increased by {}", memeId, CUSTOM_SCORE);
    }

    public void onMemeShared(Long memeId, MemeSimpleResponse meme) {
        cache.incrementScore(memeId, SHARE_SCORE, meme);
        log.debug("Meme shared: id={}, score increased by {}", memeId, SHARE_SCORE);
    }

    public List<MemeSimpleResponse> getTopPopularMemes() {
        return cache.getTopK().stream()
            .map(TopKScoredCache.CacheEntry::getValue)
            .toList();
    }

    public void initializeWithMemes(List<MemeSimpleResponse> memes) {
        for (MemeSimpleResponse meme : memes) {
            cache.incrementScore(meme.id(), VIEW_SCORE, meme);
        }
        log.debug("Initialized cache with {} memes", memes.size());
    }

    @Scheduled(fixedRate = 3600_000)
    public void evictExpiredMemes() {
        int evicted = cache.evictExpired();
        if (evicted > 0) {
            log.info("Evicted {} expired memes from popular cache", evicted);
        }
    }

    @Scheduled(fixedRate = 21600_000)
    public void cleanupStaleIndex() {
        int cleaned = cache.cleanupStaleScoreIndex();
        if (cleaned > 0) {
            log.info("Cleaned up {} stale entries from score index", cleaned);
        }
    }
}
