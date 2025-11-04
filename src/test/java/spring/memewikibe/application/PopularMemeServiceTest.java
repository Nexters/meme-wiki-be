package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PopularMemeServiceTest {

    @Mock
    private InMemoryPopularMemeCache mockInMemoryPopularMemeCache;

    @Mock
    private MemeAggregationLookUpServiceImpl mockMemeAggregationLookUpService;

    @Mock
    private MemeLookUpService mockMemeLookUpService;

    @InjectMocks
    private PopularMemeService popularMemeService;

    private List<MemeSimpleResponse> testMemes;
    private List<Long> testMemeIds;
    private List<Meme> testMemeEntities;

    @BeforeEach
    void setUp() {
        testMemes = List.of(
            new MemeSimpleResponse(1L, "무야호", "무야호.jpg"),
            new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg"),
            new MemeSimpleResponse(3L, "전남친 토스트", "전남친토스트.jpg")
        );

        testMemeIds = List.of(1L, 2L, 3L);

        testMemeEntities = List.of(
            createMeme(1L, "무야호", "무야호.jpg"),
            createMeme(2L, "나만 아니면 돼", "나만아니면돼.jpg"),
            createMeme(3L, "전남친 토스트", "전남친토스트.jpg")
        );
    }

    private Meme createMeme(Long id, String title, String imgUrl) {
        Meme meme = mock(Meme.class);
        when(meme.getId()).thenReturn(id);
        when(meme.getTitle()).thenReturn(title);
        when(meme.getImgUrl()).thenReturn(imgUrl);
        return meme;
    }

    @Test
    void getTopPopularMemes는_캐시가_충분하면_캐시에서_조회한다() {
        // given
        when(mockInMemoryPopularMemeCache.getTopPopularMemeIds()).thenReturn(testMemeIds);
        when(mockInMemoryPopularMemeCache.getTargetSize()).thenReturn(3);
        when(mockMemeLookUpService.getMemesByIds(testMemeIds)).thenReturn(testMemeEntities);

        // when
        List<MemeSimpleResponse> result = popularMemeService.getTopPopularMemes();

        // then
        then(result).hasSize(3);
        then(result.get(0).id()).isEqualTo(1L);
        then(result.get(1).id()).isEqualTo(2L);
        then(result.get(2).id()).isEqualTo(3L);
        verify(mockInMemoryPopularMemeCache).getTopPopularMemeIds();
        verify(mockInMemoryPopularMemeCache).getTargetSize();
        verify(mockMemeAggregationLookUpService, never()).getMostPopularMemes();
    }

    @Test
    void getTopPopularMemes는_캐시가_부족하면_DB에서_재조회한다() {
        // given - 캐시에 2개만 있고, 목표는 6개
        List<Long> insufficientIds = List.of(1L, 2L);
        when(mockInMemoryPopularMemeCache.getTopPopularMemeIds()).thenReturn(insufficientIds);
        when(mockInMemoryPopularMemeCache.getTargetSize()).thenReturn(6);
        when(mockMemeAggregationLookUpService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result = popularMemeService.getTopPopularMemes();

        // then
        then(result).isEqualTo(testMemes);
        verify(mockInMemoryPopularMemeCache, atLeastOnce()).getTopPopularMemeIds();
        verify(mockInMemoryPopularMemeCache, atLeastOnce()).getTargetSize();
        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
        verify(mockInMemoryPopularMemeCache).initializeWithMemeIds(testMemeIds);
    }

    @Test
    void getTopPopularMemes는_캐시가_비어있으면_DB에서_조회한다() {
        // given
        when(mockInMemoryPopularMemeCache.getTopPopularMemeIds()).thenReturn(Collections.emptyList());
        when(mockInMemoryPopularMemeCache.getTargetSize()).thenReturn(6);
        when(mockMemeAggregationLookUpService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        List<MemeSimpleResponse> result = popularMemeService.getTopPopularMemes();

        // then
        then(result).isEqualTo(testMemes);
        verify(mockInMemoryPopularMemeCache, atLeastOnce()).getTopPopularMemeIds();
        verify(mockInMemoryPopularMemeCache, atLeastOnce()).getTargetSize();
        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
    }

    @Test
    void warmUpCache는_DB에서_조회한_데이터의_ID로_캐시를_초기화한다() {
        // given
        when(mockMemeAggregationLookUpService.getMostPopularMemes()).thenReturn(testMemes);

        // when
        popularMemeService.warmUpCache();

        // then
        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
        verify(mockInMemoryPopularMemeCache).initializeWithMemeIds(testMemeIds);
    }

    @Test
    void warmUpCache는_예외가_발생해도_애플리케이션이_시작된다() {
        // given
        when(mockMemeAggregationLookUpService.getMostPopularMemes())
            .thenThrow(new RuntimeException("DB connection failed"));

        // when & then - 예외가 발생해도 메서드가 정상적으로 종료됨
        popularMemeService.warmUpCache();

        verify(mockMemeAggregationLookUpService).getMostPopularMemes();
        verify(mockInMemoryPopularMemeCache, never()).initializeWithMemeIds(any());
    }
}
