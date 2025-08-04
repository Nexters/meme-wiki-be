package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemeAggregationLookUpCacheProxyServiceTest {

    @Mock
    private MemeAggregationLookUpServiceImpl mockOriginService;

    @InjectMocks
    private MemeAggregationLookUpCacheProxyService cacheProxyService;

    private List<MemeSimpleResponse> testMemes;

    @BeforeEach
    void setUp() {
        testMemes = Arrays.asList(
            new MemeSimpleResponse(1L, "무야호", "무야호.jpg"),
            new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg"),
            new MemeSimpleResponse(3L, "전남친 토스트", "전남친토스트.jpg")
        );
    }

    @Test
    void getMostPopularMemes는_캐시가_null이면_원본_서비스를_호출하고_결과를_캐시한다() {
        // given
        when(mockOriginService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result = cacheProxyService.getMostPopularMemes();

        // then
        then(result).isEqualTo(testMemes);
        verify(mockOriginService, times(1)).getMostPopularMemes();
    }

    @Test
    void getMostPopularMemes_캐시가_있으면_원본_서비스를_호출하지_않고_캐시를_반환한다() {
        // given
        when(mockOriginService.getMostPopularMemes()).thenReturn(testMemes);
        cacheProxyService.warmUpCache();
        reset(mockOriginService);

        // when
        List<MemeSimpleResponse> result = cacheProxyService.getMostPopularMemes();

        // then
        then(result).isEqualTo(testMemes);
        verify(mockOriginService, never()).getMostPopularMemes();
    }

    @Test
    void getMostPopularMemes_여러_번_호출해도_첫_번째만_원본_서비스를_호출한다() {
        // given
        when(mockOriginService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        cacheProxyService.getMostPopularMemes();
        cacheProxyService.getMostPopularMemes();
        cacheProxyService.getMostPopularMemes();

        // then
        verify(mockOriginService, times(1)).getMostPopularMemes();
    }

    @Test
    void getMostFrequentSharedMemes는_항상_원본_서비스를_호출한다() {
        // given
        when(mockOriginService.getMostFrequentSharedMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result1 = cacheProxyService.getMostFrequentSharedMemes();
        List<MemeSimpleResponse> result2 = cacheProxyService.getMostFrequentSharedMemes();

        // then
        then(result1).isEqualTo(testMemes);
        then(result2).isEqualTo(testMemes);
        verify(mockOriginService, times(2)).getMostFrequentSharedMemes(); // 매번 호출
    }

    @Test
    void getMostFrequentCustomMemes는_항상_원본_서비스를_호출한다() {
        // given
        when(mockOriginService.getMostFrequentCustomMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result1 = cacheProxyService.getMostFrequentCustomMemes();
        List<MemeSimpleResponse> result2 = cacheProxyService.getMostFrequentCustomMemes();

        // then
        then(result1).isEqualTo(testMemes);
        then(result2).isEqualTo(testMemes);
        verify(mockOriginService, times(2)).getMostFrequentCustomMemes(); // 매번 호출
    }
}