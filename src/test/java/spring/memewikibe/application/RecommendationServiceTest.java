package spring.memewikibe.application;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.api.controller.recommendation.response.MemeRecommendationResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;
import spring.memewikibe.infrastructure.ai.NaverRagService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    MemeVectorIndexService vectorIndexService;

    @Mock
    MemeRepository memeRepository;

    @Mock
    NaverRagService naverRagService;

    @Mock
    SafeFullTextSearchExecutor safeFts;

    @InjectMocks
    RecommendationService recommendationService;

    @Captor
    ArgumentCaptor<List<NaverRagService.Candidate>> candidatesCaptor;

    // helper to assign id in tests
    private static Meme setId(Meme m, Long id) {
        try {
            var f = m.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, id);
            return m;
        } catch (Exception e) {
            return m;
        }
    }

    @Disabled("RAG 통합 이후 테스트 수정 필요")
    @Test
    void searchWithReasons_callsServices_and_respectsRagOrder_andReason() {
        // given
        String query = "집에 가고싶다";
        // Vector returns ids in this order
        when(vectorIndexService.query(anyString(), anyInt())).thenReturn(List.of(2L, 1L, 3L));

        Meme m1 = Meme.builder().title("퇴근").usageContext("퇴근하고 집 가는 상황").hashtags("[\"#귀가\",\"#퇴근\"]").flag(Meme.Flag.NORMAL).build();
        // simulate IDs 1,2,3 by saving orderless then setting via reflection is overkill; we just map by order here.
        // We'll make repository return these by the given ids order.
        // For simplicity, return all three; the service uses findAllById which doesn't guarantee order; our RAG will enforce later.
        Meme m2 = Meme.builder().title("야근").usageContext("야근 끝").hashtags("[\"#퇴근\"]").flag(Meme.Flag.NORMAL).build();
        Meme m3 = Meme.builder().title("집가자").usageContext("집 가자").hashtags("[\"#귀가\"]").flag(Meme.Flag.NORMAL).build();

        when(memeRepository.findAllById(anyIterable())).thenReturn(List.of(
            setId(m1, 1L), setId(m2, 2L), setId(m3, 3L)
        ));
        when(memeRepository.findCandidatesByFullTextSearch(anyString(), anyInt()))
            .thenReturn(List.of(setId(Meme.builder().title("귀가").usageContext("집으로 귀가").hashtags("[\"#귀가\"]").flag(Meme.Flag.NORMAL).build(), 4L)));

        // RAG reorders and returns a witty reason for top-1
        NaverRagService.RagResult rag = new NaverRagService.RagResult(List.of(1L, 3L, 2L), "귀가에 찰떡인 밈!");
        when(naverRagService.recommendWithContextDetailed(anyString(), anyString(), candidatesCaptor.capture()))
            .thenReturn(rag);

        // when
        List<MemeRecommendationResponse> out = recommendationService.searchWithReasons(query, null, 3);

        // then
        assertThat(out).extracting(MemeRecommendationResponse::id).containsExactly(1L, 3L, 2L);
        assertThat(out.get(0).reason()).isEqualTo("귀가에 찰떡인 밈!");
        // verify that NaverRagService got candidates with required fields
        List<NaverRagService.Candidate> sent = candidatesCaptor.getValue();
        assertThat(sent).isNotEmpty();
        verify(vectorIndexService, times(1)).query(anyString(), anyInt());
        verify(naverRagService, times(1)).recommendWithContextDetailed(anyString(), anyString(), anyList());
    }
}
