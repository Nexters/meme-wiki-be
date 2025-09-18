package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.admin.response.MemePopularityListResponse;
import spring.memewikibe.api.controller.admin.response.MemePopularityResponse;
import spring.memewikibe.domain.meme.MemeAggregationResult;
import spring.memewikibe.infrastructure.MemeAggregationRepository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AdminMemeStatsService {

    private final MemeAggregationRepository memeAggregationRepository;

    public AdminMemeStatsService(@Qualifier("memeAggregationRepositoryImpl") MemeAggregationRepository memeAggregationRepository) {
        this.memeAggregationRepository = memeAggregationRepository;
    }

    public MemePopularityListResponse getPopularMemes(Duration duration, int limit) {
        List<MemeAggregationResult> aggregationResults = memeAggregationRepository.findTopRatedMemesBy(duration, limit);

        AtomicInteger rank = new AtomicInteger(1);
        List<MemePopularityResponse> popularMemes = aggregationResults.stream()
            .map(result -> MemePopularityResponse.from(result, rank.getAndIncrement()))
            .toList();

        String periodDescription = formatDurationDescription(duration);
        return MemePopularityListResponse.of(popularMemes, periodDescription);
    }

    public MemePopularityListResponse getDailyPopularMemes(int limit) {
        return getPopularMemes(Duration.ofDays(1), limit);
    }

    public MemePopularityListResponse getWeeklyPopularMemes(int limit) {
        return getPopularMemes(Duration.ofDays(7), limit);
    }

    public MemePopularityListResponse getMonthlyPopularMemes(int limit) {
        return getPopularMemes(Duration.ofDays(30), limit);
    }

    private String formatDurationDescription(Duration duration) {
        long days = duration.toDays();
        if (days == 1) {
            return "일간";
        } else if (days == 7) {
            return "주간";
        } else if (days == 30) {
            return "월간";
        } else {
            return days + "일간";
        }
    }
}