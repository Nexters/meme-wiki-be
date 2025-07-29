package spring.memewikibe.infrastructure;

import spring.memewikibe.domain.meme.MemeAggregationResult;

import java.time.Duration;
import java.util.List;

public interface MemeAggregationRepository {
    List<MemeAggregationResult> findTopRatedMemesBy(Duration duration, int limit);
}
