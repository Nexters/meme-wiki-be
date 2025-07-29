package spring.memewikibe.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.infrastructure.MemeCustomLogRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;
import spring.memewikibe.infrastructure.MemeViewLogRepository;

import java.time.Duration;
import java.util.List;

@Service
public class MemeAggregationLookUpServiceImpl implements MemeAggregationLookUpService {

    private final static Duration AGGREGATION_DURATION_PERIOD = Duration.ofDays(1);
    private final static int AGGREGATION_ITEM_COUNT = 10;

    private final MemeCustomLogRepository customLogRepository;
    private final MemeViewLogRepository viewLogRepository;
    private final MemeShareLogRepository shareLogRepository;

    public MemeAggregationLookUpServiceImpl(MemeCustomLogRepository customLogRepository, MemeViewLogRepository viewLogRepository, MemeShareLogRepository shareLogRepository) {
        this.customLogRepository = customLogRepository;
        this.viewLogRepository = viewLogRepository;
        this.shareLogRepository = shareLogRepository;
    }


    @Transactional(readOnly = true)
    @Override
    public List<MemeSimpleResponse> getMostFrequentSharedMemes() {
        return shareLogRepository.findTopMemesByShareCountWithin(AGGREGATION_DURATION_PERIOD, AGGREGATION_ITEM_COUNT);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemeSimpleResponse> getMostFrequentCustomMemes() {
        return customLogRepository.findTopMemesByCustomCountWithin(AGGREGATION_DURATION_PERIOD, AGGREGATION_ITEM_COUNT);
    }

    @Override
    public List<MemeSimpleResponse> getMostPopularMemes() {
        return List.of();
    }
}
