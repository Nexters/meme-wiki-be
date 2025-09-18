package spring.memewikibe.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.MemeAggregationResult;
import spring.memewikibe.infrastructure.MemeAggregationRepository;
import spring.memewikibe.infrastructure.MemeCustomLogRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemeAggregationLookUpServiceImpl implements MemeAggregationLookUpService {

    private final static Duration AGGREGATION_DURATION_PERIOD = Duration.ofDays(7);
    private final static int AGGREGATION_ITEM_COUNT = 10;
    private final static int MOST_POPULAR_MEMES_COUNT = 6;

    private final MemeCustomLogRepository customLogRepository;
    private final MemeShareLogRepository shareLogRepository;
    private final MemeRepository memeRepository;
    private final MemeAggregationRepository memeAggregationRepository;

    public MemeAggregationLookUpServiceImpl(MemeCustomLogRepository customLogRepository, MemeShareLogRepository shareLogRepository, MemeRepository memeRepository, MemeAggregationRepository memeAggregationRepository) {
        this.customLogRepository = customLogRepository;
        this.shareLogRepository = shareLogRepository;
        this.memeRepository = memeRepository;
        this.memeAggregationRepository = memeAggregationRepository;
    }


    @Transactional(readOnly = true)
    @Override
    public List<MemeSimpleResponse> getMostFrequentSharedMemes() {
        List<MemeSimpleResponse> response = shareLogRepository.findTopMemesByShareCountWithin(AGGREGATION_DURATION_PERIOD, AGGREGATION_ITEM_COUNT);
        if (response.size() < AGGREGATION_ITEM_COUNT) {
            return fillWithLatestMemes(response, AGGREGATION_ITEM_COUNT);
        }
        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemeSimpleResponse> getMostFrequentCustomMemes() {
        List<MemeSimpleResponse> response = customLogRepository.findTopMemesByCustomCountWithin(AGGREGATION_DURATION_PERIOD, AGGREGATION_ITEM_COUNT);
        if (response.size() < AGGREGATION_ITEM_COUNT) {
            return fillWithLatestMemes(response, AGGREGATION_ITEM_COUNT);
        }
        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemeSimpleResponse> getMostPopularMemes() {
        List<MemeAggregationResult> aggregationResult = memeAggregationRepository.findTopRatedMemesBy(AGGREGATION_DURATION_PERIOD, MOST_POPULAR_MEMES_COUNT);
        List<MemeSimpleResponse> response = aggregationResult.stream()
            .map(it -> new MemeSimpleResponse(it.id(), it.title(), it.imgUrl()))
            .toList();
        if (response.size() < MOST_POPULAR_MEMES_COUNT) {
            return fillWithLatestMemes(response, MOST_POPULAR_MEMES_COUNT);
        }
        return response;
    }

    private List<MemeSimpleResponse> fillWithLatestMemes(List<MemeSimpleResponse> originalList, int targetCount) {
        int remainingCount = targetCount - originalList.size();
        List<Long> existingIds = originalList.stream().map(MemeSimpleResponse::id).toList();
        List<MemeSimpleResponse> latestMemes = memeRepository.findLatestMemesExcludingIds(existingIds, remainingCount);

        List<MemeSimpleResponse> result = new ArrayList<>(originalList);
        result.addAll(latestMemes);
        return result;
    }
}
