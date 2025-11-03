package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularMemeServiceTest {

    @Mock
    private PopularMemeCache mockPopularMemeCache;

    @Mock
    private MemeAggregationLookUpServiceImpl mockMemeAggregationLookUpService;

    @InjectMocks
    private PopularMemeService popularMemeService;

    private List<MemeSimpleResponse> testMemes;

    @BeforeEach
    void setUp() {
        testMemes = List.of(
            new MemeSimpleResponse(1L, "무야호", "무야호.jpg"),
            new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg"),
            new MemeSimpleResponse(3L, "전남친 토스트", "전남친토스트.jpg")
        );
    }

    @Test
    void getTopPopularMemes는_캐시에_데이터가_있으면_캐시를_반환한다() {
        // given
        when(mockPopularMemeCache.getTopPopularMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result = popularMemeService.getTopPopularMemes();

        // then
        then(result).isEqualTo(testMemes);
        verify(mockPopularMemeCache).getTopPopularMemes();
        verify(mockMemeAggregationLookUpService, never()).getMostPopularMemes();
    }

    @Test
    void getTopPopularMemes는_캐시가_비어있으면_DB에서_조회한다() {
        // given
        when(mockPopularMemeCache.getTopPopularMemes()).thenReturn(Collections.emptyList());
        when(mockMemeAggregationLookUpService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result = popularMemeService.getTopPopularMemes();

        // then
        then(result).isEqualTo(testMemes);
        verify(mockPopularMemeCache).getTopPopularMemes();
        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
    }

    @Test
    void getTopPopularMemes는_캐시가_있으면_여러_번_호출해도_DB를_호출하지_않는다() {
        // given
        when(mockPopularMemeCache.getTopPopularMemes()).thenReturn(testMemes);

        // when
        popularMemeService.getTopPopularMemes();
        popularMemeService.getTopPopularMemes();
        popularMemeService.getTopPopularMemes();

        // then
        verify(mockPopularMemeCache, times(3)).getTopPopularMemes();
        verify(mockMemeAggregationLookUpService, never()).getMostPopularMemes();
    }

    @Test
    void warmUpCache는_DB에서_조회한_데이터로_캐시를_초기화한다() {
        // given
        when(mockMemeAggregationLookUpService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        popularMemeService.warmUpCache();

        // then
        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
        verify(mockPopularMemeCache).initializeWithMemes(testMemes);
    }

    @Test
    void warmUpCache는_예외가_발생해도_애플리케이션이_시작된다() {
        // given
        when(mockMemeAggregationLookUpService.getMostPopularMemes())
            .thenThrow(new RuntimeException("DB connection failed"));

        // when & then - 예외가 발생해도 메서드가 정상적으로 종료됨
        popularMemeService.warmUpCache();

        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
        verify(mockPopularMemeCache, never()).initializeWithMemes(any());
    }
}
