package spring.memewikibe.application;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Service
public class PopularMemeService {

    private final PopularMemeCache popularMemeCache;
    private final MemeAggregationLookUpServiceImpl memeAggregationLookUpServiceImpl;

    public List<MemeSimpleResponse> getTopPopularMemes() {
        List<MemeSimpleResponse> cachedMemes = popularMemeCache.getTopPopularMemes();

        if (cachedMemes.isEmpty()) {
            log.debug("Cache is empty, falling back to DB");
            return memeAggregationLookUpServiceImpl.getMostPopularMemes();
        }

        return cachedMemes;
    }

    @PostConstruct
    public void warmUpCache() {
        try {
            List<MemeSimpleResponse> popularMemes = memeAggregationLookUpServiceImpl.getMostPopularMemes();
            popularMemeCache.initializeWithMemes(popularMemes);
            log.info("Popular meme cache warmed up with {} memes from DB", popularMemes.size());
        } catch (Exception e) {
            log.error("Failed to warm up popular meme cache", e);
        }
    }
}
