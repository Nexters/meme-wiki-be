package spring.memewikibe.application;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.api.controller.meme.response.MostSharedMemes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SharedMemeScheduleCacheService {

    private static final String CRON_EXPRESSION = "0 0 4 * * *";

    private final CronExpression cronExpression = CronExpression.parse(CRON_EXPRESSION);
    private final MemeAggregationLookUpService memeAggregationLookUpService;

    private volatile MostSharedMemes cachedData;

    public SharedMemeScheduleCacheService(MemeAggregationLookUpServiceImpl memeAggregationLookUpService) {
        this.memeAggregationLookUpService = memeAggregationLookUpService;
    }

    public MostSharedMemes getMostSharedMemes() {
        if (cachedData == null) {
            refreshCache();
        }
        return cachedData;
    }

    @PostConstruct
    public void initializeCache() {
        log.info("Initializing shared meme cache");
        refreshCache();
    }

    @Scheduled(cron = CRON_EXPRESSION)
    public void scheduledRefresh() {
        log.info("Scheduled refresh of shared meme cache");
        refreshCache();
    }

    private void refreshCache() {
        try {
            List<MemeSimpleResponse> memes = memeAggregationLookUpService.getMostFrequentSharedMemes();
            LocalDateTime nextUpdateTime = cronExpression.next(LocalDateTime.now());

            cachedData = new MostSharedMemes(memes, nextUpdateTime);

            log.info("Shared meme cache refreshed. Next update at: {}", nextUpdateTime);
        } catch (Exception e) {
            log.error("Failed to refresh shared meme cache", e);
        }
    }
}