package spring.memewikibe.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.admin.response.MemePopularityListResponse;
import spring.memewikibe.domain.meme.MemeAggregationResult;
import spring.memewikibe.infrastructure.MemeAggregationRepository;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemeStatsServiceTest {

    @Mock
    private MemeAggregationRepository memeAggregationRepository;

    @InjectMocks
    private AdminMemeStatsService adminMemeStatsService;

    @Test
    void getPopularMemes는_인기_밈_목록을_반환한다() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 10;
        List<MemeAggregationResult> mockResults = List.of(
            new MemeAggregationResult(1L, "밈1", "url1", 100L, 50L, 20L, 210L),
            new MemeAggregationResult(2L, "밈2", "url2", 80L, 40L, 15L, 165L)
        );
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit)).thenReturn(mockResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        assertThat(response.popularMemes()).hasSize(2);
        assertThat(response.period()).isEqualTo("주간");
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.popularMemes().get(0).rank()).isEqualTo(1);
        assertThat(response.popularMemes().get(1).rank()).isEqualTo(2);
        verify(memeAggregationRepository).findTopRatedMemesBy(duration, limit);
    }

    @Test
    void getPopularMemes는_빈_목록을_처리한다() {
        // given
        Duration duration = Duration.ofDays(1);
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit)).thenReturn(List.of());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        assertThat(response.popularMemes()).isEmpty();
        assertThat(response.totalCount()).isZero();
    }

    @Test
    void getPopularMemes는_null_duration을_거부한다() {
        // when & then
        assertThatThrownBy(() -> adminMemeStatsService.getPopularMemes(null, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duration cannot be null");
    }

    @Test
    void getPopularMemes는_음수_duration을_거부한다() {
        // when & then
        assertThatThrownBy(() -> adminMemeStatsService.getPopularMemes(Duration.ofDays(-1), 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duration must be positive");
    }

    @Test
    void getPopularMemes는_0_duration을_거부한다() {
        // when & then
        assertThatThrownBy(() -> adminMemeStatsService.getPopularMemes(Duration.ZERO, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duration must be positive");
    }

    @Test
    void getPopularMemes는_0_limit을_거부한다() {
        // when & then
        assertThatThrownBy(() -> adminMemeStatsService.getPopularMemes(Duration.ofDays(7), 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Limit must be positive");
    }

    @Test
    void getPopularMemes는_음수_limit을_거부한다() {
        // when & then
        assertThatThrownBy(() -> adminMemeStatsService.getPopularMemes(Duration.ofDays(7), -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Limit must be positive");
    }

    @Test
    void getDailyPopularMemes는_일간_인기_밈을_반환한다() {
        // given
        int limit = 20;
        List<MemeAggregationResult> mockResults = List.of(
            new MemeAggregationResult(1L, "밈1", "url1", 50L, 20L, 10L, 90L)
        );
        when(memeAggregationRepository.findTopRatedMemesBy(Duration.ofDays(1), limit))
            .thenReturn(mockResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getDailyPopularMemes(limit);

        // then
        assertThat(response.period()).isEqualTo("일간");
        assertThat(response.popularMemes()).hasSize(1);
        verify(memeAggregationRepository).findTopRatedMemesBy(Duration.ofDays(1), limit);
    }

    @Test
    void getWeeklyPopularMemes는_주간_인기_밈을_반환한다() {
        // given
        int limit = 20;
        List<MemeAggregationResult> mockResults = List.of(
            new MemeAggregationResult(1L, "밈1", "url1", 100L, 50L, 20L, 210L)
        );
        when(memeAggregationRepository.findTopRatedMemesBy(Duration.ofDays(7), limit))
            .thenReturn(mockResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getWeeklyPopularMemes(limit);

        // then
        assertThat(response.period()).isEqualTo("주간");
        assertThat(response.popularMemes()).hasSize(1);
        verify(memeAggregationRepository).findTopRatedMemesBy(Duration.ofDays(7), limit);
    }

    @Test
    void getMonthlyPopularMemes는_월간_인기_밈을_반환한다() {
        // given
        int limit = 20;
        List<MemeAggregationResult> mockResults = List.of(
            new MemeAggregationResult(1L, "밈1", "url1", 500L, 200L, 100L, 1000L)
        );
        when(memeAggregationRepository.findTopRatedMemesBy(Duration.ofDays(30), limit))
            .thenReturn(mockResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getMonthlyPopularMemes(limit);

        // then
        assertThat(response.period()).isEqualTo("월간");
        assertThat(response.popularMemes()).hasSize(1);
        verify(memeAggregationRepository).findTopRatedMemesBy(Duration.ofDays(30), limit);
    }

    @Test
    void getPopularMemes는_커스텀_기간의_설명을_생성한다() {
        // given
        Duration customDuration = Duration.ofDays(14);
        int limit = 10;
        when(memeAggregationRepository.findTopRatedMemesBy(customDuration, limit))
            .thenReturn(List.of());

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(customDuration, limit);

        // then
        assertThat(response.period()).isEqualTo("14일간");
    }

    @Test
    void getPopularMemes는_순위를_올바르게_부여한다() {
        // given
        Duration duration = Duration.ofDays(7);
        int limit = 5;
        List<MemeAggregationResult> mockResults = List.of(
            new MemeAggregationResult(1L, "밈1", "url1", 100L, 50L, 20L, 210L),
            new MemeAggregationResult(2L, "밈2", "url2", 90L, 45L, 18L, 189L),
            new MemeAggregationResult(3L, "밈3", "url3", 80L, 40L, 15L, 165L),
            new MemeAggregationResult(4L, "밈4", "url4", 70L, 35L, 12L, 141L),
            new MemeAggregationResult(5L, "밈5", "url5", 60L, 30L, 10L, 120L)
        );
        when(memeAggregationRepository.findTopRatedMemesBy(duration, limit)).thenReturn(mockResults);

        // when
        MemePopularityListResponse response = adminMemeStatsService.getPopularMemes(duration, limit);

        // then
        assertThat(response.popularMemes()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(response.popularMemes().get(i).rank()).isEqualTo(i + 1);
        }
    }
}
