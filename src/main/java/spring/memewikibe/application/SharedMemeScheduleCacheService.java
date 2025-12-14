package spring.memewikibe.application;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.api.controller.meme.response.MostSharedMemes;
import spring.memewikibe.common.util.TimeProvider;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SharedMemeScheduleCacheService {

    private static final String SHARED_MEME_RENEWAL_CRON = "0 0 4 * * *";

    private final CronExpression cronExpression = CronExpression.parse(SHARED_MEME_RENEWAL_CRON);
    private final MemeAggregationLookUpService memeAggregationLookUpService;
    private final TimeProvider timeProvider;

    private volatile MostSharedMemes cachedData;

    public MostSharedMemes getMostSharedMemes() {
        if (cachedData == null) {
            synchronized (this) {
                if (cachedData == null) {
                    refreshCache();
                }
            }
        }
        return cachedData;
    }

    @PostConstruct
    public void initializeCache() {
        log.info("Initializing shared meme cache");
        refreshCache();
    }

    @Scheduled(cron = SHARED_MEME_RENEWAL_CRON)
    public void scheduledRefresh() {
        log.info("Scheduled refresh of shared meme cache");
        refreshCache();
    }

    private synchronized void refreshCache() {
        try {
            List<MemeSimpleResponse> memes = memeAggregationLookUpService.getMostFrequentSharedMemes();
            LocalDateTime nextUpdateTime = cronExpression.next(timeProvider.now());

            cachedData = new MostSharedMemes(memes, nextUpdateTime);

            log.info("Shared meme cache refreshed. Next update at: {}", nextUpdateTime);
        } catch (Exception e) {
            log.error("Failed to refresh shared meme cache", e);
            if (cachedData == null) {
                LocalDateTime nextUpdateTime = cronExpression.next(timeProvider.now());
                cachedData = new MostSharedMemes(List.of(), nextUpdateTime);
                log.warn("Initialized cache with empty meme list due to refresh failure");
            }
        }
    }
}