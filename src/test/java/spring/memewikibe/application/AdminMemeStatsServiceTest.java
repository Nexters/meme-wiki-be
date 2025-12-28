package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.api.controller.admin.response.MemePopularityListResponse;
import spring.memewikibe.api.controller.admin.response.MemePopularityResponse;
import spring.memewikibe.domain.meme.MemeAggregationResult;
import spring.memewikibe.infrastructure.MemeAggregationRepository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AdminMemeStatsServiceTest {

    @Mock
    private MemeAggregationRepository memeAggregationRepository;

    @InjectMocks
    private AdminMemeStatsService adminMemeStatsService;

    private List<MemeAggregationResult> sampleResults;

    @BeforeEach
    void setUp() {
        sampleResults = List.of(
            new MemeAggregationResult(1L, "무야호", "https://example.com/muyaho.jpg", 1000L, 500L, 100L, 1700L),
            new MemeAggregationResult(2L, "나만 아니면 돼", "https://example.com/not-me.jpg", 800L, 300L, 50L, 1250L),
            new MemeAggregationResult(3L, "전남친 토스트", "https://example.com/toast.jpg", 500L, 200L, 30L, 820L)
        );
    }

    @Test
    @DisplayName("getPopularMemes: 지정된 기간과 제한으로 인기 밈을 조회한다")
    void getPopularMemes_returnsPopularMemesWithRankings() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 3;
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
            .thenReturn(sampleResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        then(response).isNotNull();
        then(response.popularMemes()).hasSize(3);
        then(response.totalCount()).isEqualTo(3);
        then(response.period()).isEqualTo("주간");

        verify(memeAggregationRepository).findTopRatedMemesBy(duration, limit);
    }

    @Test
    @DisplayName("getPopularMemes: 순위가 올바르게 할당된다")
    void getPopularMemes_assignsCorrectRankings() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 3;
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
            .thenReturn(sampleResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        List<MemePopularityResponse> popularMemes = response.popularMemes();
        then(popularMemes.get(0).rank()).isEqualTo(1);
        then(popularMemes.get(1).rank()).isEqualTo(2);
        then(popularMemes.get(2).rank()).isEqualTo(3);
    }

    @Test
    @DisplayName("getPopularMemes: 빈 결과를 올바르게 처리한다")
    void getPopularMemes_handlesEmptyResults() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
            .thenReturn(Collections.emptyList());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        then(response).isNotNull();
        then(response.popularMemes()).isEmpty();
        then(response.totalCount()).isZero();
        then(response.period()).isEqualTo("주간");
    }

    @Test
    @DisplayName("getPopularMemes: 결과가 repository에서 반환된 데이터를 올바르게 매핑한다")
    void getPopularMemes_correctlyMapsRepositoryData() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 3;
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
            .thenReturn(sampleResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        List<MemePopularityResponse> popularMemes = response.popularMemes();

        MemePopularityResponse first = popularMemes.get(0);
        then(first.id()).isEqualTo(1L);
        then(first.title()).isEqualTo("무야호");
        then(first.imgUrl()).isEqualTo("https://example.com/muyaho.jpg");
        then(first.viewCount()).isEqualTo(1000L);
        then(first.shareCount()).isEqualTo(500L);
        then(first.customCount()).isEqualTo(100L);
        then(first.totalScore()).isEqualTo(1700L);
    }

    @Test
    @DisplayName("getDailyPopularMemes: 일간 인기 밈을 조회한다")
    void getDailyPopularMemes_callsGetPopularMemesWithOneDay() {
        // given
        int limit = 20;
        when(memeAggregationRepository.findTopRatedMemesBy(Duration.ofDays(1), limit))
            .thenReturn(sampleResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getDailyPopularMemes(limit);

        // then
        then(response).isNotNull();
        then(response.period()).isEqualTo("일간");
        verify(memeAggregationRepository).findTopRatedMemesBy(Duration.ofDays(1), limit);
    }

    @Test
    @DisplayName("getWeeklyPopularMemes: 주간 인기 밈을 조회한다")
    void getWeeklyPopularMemes_callsGetPopularMemesWithSevenDays() {
        // given
        int limit = 20;
        when(memeAggregationRepository.findTopRatedMemesBy(Duration.ofDays(7), limit))
            .thenReturn(sampleResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getWeeklyPopularMemes(limit);

        // then
        then(response).isNotNull();
        then(response.period()).isEqualTo("주간");
        verify(memeAggregationRepository).findTopRatedMemesBy(Duration.ofDays(7), limit);
    }

    @Test
    @DisplayName("getMonthlyPopularMemes: 월간 인기 밈을 조회한다")
    void getMonthlyPopularMemes_callsGetPopularMemesWithThirtyDays() {
        // given
        int limit = 20;
        when(memeAggregationRepository.findTopRatedMemesBy(Duration.ofDays(30), limit))
            .thenReturn(sampleResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getMonthlyPopularMemes(limit);

        // then
        then(response).isNotNull();
        then(response.period()).isEqualTo("월간");
        verify(memeAggregationRepository).findTopRatedMemesBy(Duration.ofDays(30), limit);
    }

    @Test
    @DisplayName("formatDurationDescription: 1일을 '일간'으로 포맷한다")
    void formatDurationDescription_formatsOneDayCorrectly() {
        // given
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(any(Duration.class), eq(limit)))
            .thenReturn(Collections.emptyList());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(Duration.ofDays(1), limit);

        // then
        then(response.period()).isEqualTo("일간");
    }

    @Test
    @DisplayName("formatDurationDescription: 7일을 '주간'으로 포맷한다")
    void formatDurationDescription_formatsSevenDaysCorrectly() {
        // given
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(any(Duration.class), eq(limit)))
            .thenReturn(Collections.emptyList());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(Duration.ofDays(7), limit);

        // then
        then(response.period()).isEqualTo("주간");
    }

    @Test
    @DisplayName("formatDurationDescription: 30일을 '월간'으로 포맷한다")
    void formatDurationDescription_formatsThirtyDaysCorrectly() {
        // given
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(any(Duration.class), eq(limit)))
            .thenReturn(Collections.emptyList());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(Duration.ofDays(30), limit);

        // then
        then(response.period()).isEqualTo("월간");
    }

    @Test
    @DisplayName("formatDurationDescription: 커스텀 기간을 'N일간'으로 포맷한다")
    void formatDurationDescription_formatsCustomDaysCorrectly() {
        // given
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(any(Duration.class), eq(limit)))
            .thenReturn(Collections.emptyList());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(Duration.ofDays(14), limit);

        // then
        then(response.period()).isEqualTo("14일간");
    }

    @Test
    @DisplayName("getPopularMemes: 다양한 limit 값으로 호출할 수 있다")
    void getPopularMemes_worksWithDifferentLimits() {
        // given
        Duration duration = Duration.ofDays(7);
        int[] limits = {5, 10, 20, 50, 100};

        for (int limit : limits) {
            when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
                .thenReturn(sampleResults);

            // when
            MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

            // then
            then(response).isNotNull();
            verify(memeAggregationRepository).findTopRatedMemesBy(duration, limit);
        }
    }

    @Test
    @DisplayName("getPopularMemes: 단일 결과를 올바르게 처리한다")
    void getPopularMemes_handlesSingleResult() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 1;
        List<MemeAggregationResult> singleResult = List.of(sampleResults.get(0));
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
            .thenReturn(singleResult);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        then(response.popularMemes()).hasSize(1);
        then(response.totalCount()).isEqualTo(1);
        then(response.popularMemes().get(0).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("getPopularMemes: 대량 결과를 올바르게 처리한다")
    void getPopularMemes_handlesLargeResults() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 100;
        List<MemeAggregationResult> largeResults = java.util.stream.IntStream.range(1, 101)
            .mapToObj(i -> new MemeAggregationResult(
                (long) i,
                "밈 " + i,
                "https://example.com/meme" + i + ".jpg",
                (long) (1000 - i * 10),
                (long) (500 - i * 5),
                (long) (100 - i),
                (long) (1700 - i * 17)
            ))
            .toList();
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit))
            .thenReturn(largeResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        then(response.popularMemes()).hasSize(100);
        then(response.totalCount()).isEqualTo(100);
        then(response.popularMemes().get(0).rank()).isEqualTo(1);
        then(response.popularMemes().get(99).rank()).isEqualTo(100);
    }
}
