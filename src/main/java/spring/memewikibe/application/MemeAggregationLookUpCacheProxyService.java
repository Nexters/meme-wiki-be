package spring.memewikibe.application;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.util.List;

@Slf4j
@Service
public class MemeAggregationLookUpCacheProxyService implements MemeAggregationLookUpService {

    private final MemeAggregationLookUpService memeAggregationLookUpService;

    private volatile List<MemeSimpleResponse> mostPopularMemesCache;

    public MemeAggregationLookUpCacheProxyService(MemeAggregationLookUpServiceImpl memeAggregationLookUpService) {
        this.memeAggregationLookUpService = memeAggregationLookUpService;
    }

    @Override
    public List<MemeSimpleResponse> getMostFrequentSharedMemes() {
        return memeAggregationLookUpService.getMostFrequentSharedMemes();
    }

    @Override
    public List<MemeSimpleResponse> getMostFrequentCustomMemes() {
        return memeAggregationLookUpService.getMostFrequentCustomMemes();
    }

    @Override
    public List<MemeSimpleResponse> getMostPopularMemes() {
        if (mostPopularMemesCache == null) {
            mostPopularMemesCache = memeAggregationLookUpService.getMostPopularMemes();
        }
        return mostPopularMemesCache;
    }

    @PostConstruct
    public void warmUpCache() {
        mostPopularMemesCache = memeAggregationLookUpService.getMostPopularMemes();
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void refreshCache() {
        log.info("Refreshing most popular memes cache");
        mostPopularMemesCache = memeAggregationLookUpService.getMostPopularMemes();
        log.info("Cache refreshed successfully");
    }
}
