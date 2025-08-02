package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeAggregationResult;

import java.time.Duration;
import java.util.List;

public interface MemeAggregationRepository {
    List<MemeAggregationResult> findTopRatedMemesBy(Duration duration, int limit);

    List<Meme> findByTitleDynamicContainingOrderByIdDesc(String title, Limit limit);

    List<Meme> findByTitleDynamicContainingAndIdLessThanOrderByIdDesc(String title, Long lastId, Limit limit);
}
