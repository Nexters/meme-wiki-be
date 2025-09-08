package spring.memewikibe.application;

import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.api.controller.meme.response.MostSharedMemes;
import spring.memewikibe.common.util.TimeProvider;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedMemeScheduleCacheServiceTest {

    private final LocalDateTime FIXED_DATE_TIME = LocalDateTime.of(2025, 9, 6, 11, 50);
    private final StubTimeProvider stubTimeProvider = new StubTimeProvider(FIXED_DATE_TIME);
    @Mock
    private MemeAggregationLookUpServiceImpl mockOriginService;
    private SharedMemeScheduleCacheService sharedMemeScheduleCacheService;
    private List<MemeSimpleResponse> testMemes;

    @BeforeEach
    void setUp() {
        testMemes = Arrays.asList(
            new MemeSimpleResponse(1L, "무야호", "무야호.jpg"),
            new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg"),
            new MemeSimpleResponse(3L, "전남친 토스트", "전남친토스트.jpg")
        );
        sharedMemeScheduleCacheService = new SharedMemeScheduleCacheService(mockOriginService, stubTimeProvider);
    }

    @Test
    void getMostSharedMemes는_캐시가_null이면_원본_서비스를_호출하고_결과를_캐시한다() {
        // given
        when(mockOriginService.getMostFrequentSharedMemes()).thenReturn(testMemes);

        // when
        MostSharedMemes result = sharedMemeScheduleCacheService.getMostSharedMemes();

        // then
        then(result.memes()).isEqualTo(testMemes);
        then(result.nextFetchTime()).isAfter(FIXED_DATE_TIME);
        verify(mockOriginService, times(1)).getMostFrequentSharedMemes();
    }

    @Test
    void getMostSharedMemes_캐시가_있으면_원본_서비스를_호출하지_않고_캐시를_반환한다() {
        // given
        when(mockOriginService.getMostFrequentSharedMemes()).thenReturn(testMemes);
        sharedMemeScheduleCacheService.initializeCache();
        reset(mockOriginService);

        // when
        MostSharedMemes result = sharedMemeScheduleCacheService.getMostSharedMemes();

        // then
        then(result.memes()).isEqualTo(testMemes);
        then(result.nextFetchTime()).isAfter(FIXED_DATE_TIME);
        verify(mockOriginService, never()).getMostFrequentSharedMemes();
    }

    @Test
    void getMostSharedMemes_여러_번_호출해도_첫_번째만_원본_서비스를_호출한다() {
        // given
        when(mockOriginService.getMostFrequentSharedMemes()).thenReturn(testMemes);

        // when
        MostSharedMemes result1 = sharedMemeScheduleCacheService.getMostSharedMemes();
        MostSharedMemes result2 = sharedMemeScheduleCacheService.getMostSharedMemes();
        MostSharedMemes result3 = sharedMemeScheduleCacheService.getMostSharedMemes();

        // then
        then(result1.memes()).isEqualTo(testMemes);
        then(result2.memes()).isEqualTo(testMemes);
        then(result3.memes()).isEqualTo(testMemes);
        then(result1.nextFetchTime()).isEqualTo(result2.nextFetchTime());
        then(result2.nextFetchTime()).isEqualTo(result3.nextFetchTime());
        verify(mockOriginService, times(1)).getMostFrequentSharedMemes();
    }

    @Test
    void nextFetchTime은_현재시각보다_이후의_cronjob이_동작하는시간이다() {
        // given
        when(mockOriginService.getMostFrequentSharedMemes()).thenReturn(testMemes);

        // when
        MostSharedMemes result = sharedMemeScheduleCacheService.getMostSharedMemes();

        // then
        then(result.nextFetchTime()).isAfter(FIXED_DATE_TIME);
    }

    @Test
    void scheduledRefresh가_호출되면_캐시가_갱신된다() {
        // given
        List<MemeSimpleResponse> updatedMemes = Arrays.asList(
            new MemeSimpleResponse(4L, "새로운 밈", "새로운밈.jpg")
        );
        when(mockOriginService.getMostFrequentSharedMemes())
            .thenReturn(testMemes)
            .thenReturn(updatedMemes);

        // when
        MostSharedMemes beforeRefresh = sharedMemeScheduleCacheService.getMostSharedMemes();
        sharedMemeScheduleCacheService.scheduledRefresh();
        MostSharedMemes afterRefresh = sharedMemeScheduleCacheService.getMostSharedMemes();

        // then
        then(beforeRefresh.memes()).isEqualTo(testMemes);
        then(afterRefresh.memes()).isEqualTo(updatedMemes);
        verify(mockOriginService, times(2)).getMostFrequentSharedMemes();
    }

    @Test
    void initializeCache가_호출되면_캐시가_초기화된다() {
        // given
        when(mockOriginService.getMostFrequentSharedMemes()).thenReturn(testMemes);

        // when
        sharedMemeScheduleCacheService.initializeCache();

        // then
        MostSharedMemes result = sharedMemeScheduleCacheService.getMostSharedMemes();
        then(result.memes()).isEqualTo(testMemes);
        verify(mockOriginService, times(1)).getMostFrequentSharedMemes();
    }

    public static class StubTimeProvider implements TimeProvider {
        private final LocalDateTime fixedTime;

        public StubTimeProvider(LocalDateTime fixedTime) {
            this.fixedTime = fixedTime;
        }

        @Override
        public LocalDateTime now() {
            return fixedTime;
        }

    }
}