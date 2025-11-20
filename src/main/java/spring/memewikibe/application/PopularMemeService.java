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

    private final InMemoryPopularMemeCache inMemoryPopularMemeCache;
    private final MemeAggregationLookUpService memeAggregationLookUpService;
    private final MemeLookUpService memeLookUpService;

    public List<MemeSimpleResponse> getTopPopularMemes() {
        List<Long> cachedMemeIds = inMemoryPopularMemeCache.getTopPopularMemeIds();

        if (cachedMemeIds.size() < inMemoryPopularMemeCache.getTargetSize()) {
            log.debug("Cache is not full ({}/<{}), falling back to DB",
                cachedMemeIds.size(), inMemoryPopularMemeCache.getTargetSize());
            List<MemeSimpleResponse> popularMemes = memeAggregationLookUpService.getMostPopularMemes();
            List<Long> memeIds = popularMemes.stream()
                .map(MemeSimpleResponse::id)
                .toList();
            inMemoryPopularMemeCache.initializeWithMemeIds(memeIds);
            return popularMemes;
        }

        return memeLookUpService.getOrderedMemesByIds(cachedMemeIds)
            .stream()
            .map(it -> new MemeSimpleResponse(it.getId(), it.getTitle(), it.getImgUrl()))
            .toList();
    }

    @PostConstruct
    public void warmUpCache() {
        try {
            List<MemeSimpleResponse> popularMemes = memeAggregationLookUpService.getMostPopularMemes();
            List<Long> memeIds = popularMemes.stream()
                .map(MemeSimpleResponse::id)
                .toList();
            inMemoryPopularMemeCache.initializeWithMemeIds(memeIds);
            log.info("Popular meme cache warmed up with {} meme IDs from DB", memeIds.size());
        } catch (Exception e) {
            log.error("Failed to warm up popular meme cache", e);
        }
    }
}
