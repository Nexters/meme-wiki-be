package spring.memewikibe.application;

import jakarta.annotation.PostConstruct;
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
@Service
public class SharedMemeScheduleCacheService {

    private static final String SHARED_MEME_RENEWAL_CRON = "0 0 4 * * *";

    private final CronExpression cronExpression = CronExpression.parse(SHARED_MEME_RENEWAL_CRON);
    private final MemeAggregationLookUpService memeAggregationLookUpService;
    private final TimeProvider timeProvider;

    private volatile MostSharedMemes cachedData;

    public SharedMemeScheduleCacheService(MemeAggregationLookUpService memeAggregationLookUpService, TimeProvider timeProvider) {
        this.memeAggregationLookUpService = memeAggregationLookUpService;
        this.timeProvider = timeProvider;
    }

    public MostSharedMemes getMostSharedMemes() {
        if (cachedData == null) {
            refreshCache(timeProvider);
        }
        return cachedData;
    }

    @PostConstruct
    public void initializeCache() {
        log.info("Initializing shared meme cache");
        refreshCache(timeProvider);
    }

    @Scheduled(cron = SHARED_MEME_RENEWAL_CRON)
    public void scheduledRefresh() {
        log.info("Scheduled refresh of shared meme cache");
        refreshCache(timeProvider);
    }

    private void refreshCache(TimeProvider timeProvider) {
        try {
            List<MemeSimpleResponse> memes = memeAggregationLookUpService.getMostFrequentSharedMemes();
            LocalDateTime nextUpdateTime = cronExpression.next(timeProvider.now());

            cachedData = new MostSharedMemes(memes, nextUpdateTime);

            log.info("Shared meme cache refreshed. Next update at: {}", nextUpdateTime);
        } catch (Exception e) {
            log.error("Failed to refresh shared meme cache", e);
        }
    }
}