package spring.memewikibe.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.meme.response.MemeRerankerResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.NaverClovaClient;
import spring.memewikibe.external.request.ClovaRerankerRequest;
import spring.memewikibe.external.response.ClovaRerankerResponse;
import spring.memewikibe.infrastructure.MemeRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemeSearchServiceTest {

    @Mock
    private NaverClovaClient naverClovaClient;

    @Mock
    private MemeRepository memeRepository;

    @InjectMocks
    private MemeSearchService memeSearchService;

    @Test
    @DisplayName("getRerankerMeme: 정상적인 검색 결과를 반환한다")
    void getRerankerMeme_returns_success_response_when_memes_found() {
        // given
        String query = "무야호";
        Meme meme1 = mock(Meme.class);
        Meme meme2 = mock(Meme.class);

        when(meme1.getId()).thenReturn(1L);
        when(meme1.getTitle()).thenReturn("무야호");
        when(meme1.getOrigin()).thenReturn("무한도전");
        when(meme1.getUsageContext()).thenReturn("");
        when(meme1.getTrendPeriod()).thenReturn("");
        when(meme1.getHashtags()).thenReturn("");

        when(meme2.getId()).thenReturn(2L);
        when(meme2.getTitle()).thenReturn("어쩔티비");
        when(meme2.getOrigin()).thenReturn("유행어");
        when(meme2.getUsageContext()).thenReturn("");
        when(meme2.getTrendPeriod()).thenReturn("");
        when(meme2.getHashtags()).thenReturn("");

        List<Meme> candidateMemes = List.of(meme1, meme2);

        ClovaRerankerResponse.Result.Usage usage = new ClovaRerankerResponse.Result.Usage(100, 50, 150);
        ClovaRerankerResponse.Result result = new ClovaRerankerResponse.Result(
            "검색 결과",
            List.of(),  // We're just testing that the service works, not the exact document transformation
            List.of("추천 쿼리1", "추천 쿼리2"),
            usage
        );
        ClovaRerankerResponse clovaResponse = new ClovaRerankerResponse(
            new ClovaRerankerResponse.Status("200", "OK"),
            result
        );

        when(memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL)).thenReturn(candidateMemes);
        when(naverClovaClient.reranker(any(ClovaRerankerRequest.class))).thenReturn(clovaResponse);

        // when
        MemeRerankerResponse response = memeSearchService.getRerankerMeme(query);

        // then
        then(response).isNotNull();
        then(response.result()).isEqualTo("검색 결과");
        then(response.suggestedQueries()).containsExactly("추천 쿼리1", "추천 쿼리2");

        verify(memeRepository).findByFlagOrderByIdDesc(Meme.Flag.NORMAL);
        verify(naverClovaClient).reranker(any(ClovaRerankerRequest.class));
    }

    @Test
    @DisplayName("getRerankerMeme: 검색 결과가 없으면 FAILURE를 반환한다")
    void getRerankerMeme_returns_failure_response_when_no_memes_found() {
        // given
        String query = "존재하지 않는 밈";
        Meme meme1 = mock(Meme.class);

        when(meme1.getId()).thenReturn(1L);
        when(meme1.getTitle()).thenReturn("무야호");
        when(meme1.getOrigin()).thenReturn("무한도전");
        when(meme1.getUsageContext()).thenReturn("");
        when(meme1.getTrendPeriod()).thenReturn("");
        when(meme1.getHashtags()).thenReturn("");

        List<Meme> candidateMemes = List.of(meme1);

        ClovaRerankerResponse.Result.Usage usage = new ClovaRerankerResponse.Result.Usage(100, 50, 150);
        ClovaRerankerResponse.Result result = new ClovaRerankerResponse.Result(
            "검색 결과 없음",
            Collections.emptyList(),  // No cited documents = not found
            List.of("추천 쿼리1"),
            usage
        );
        ClovaRerankerResponse clovaResponse = new ClovaRerankerResponse(
            new ClovaRerankerResponse.Status("200", "OK"),
            result
        );

        when(memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL)).thenReturn(candidateMemes);
        when(naverClovaClient.reranker(any(ClovaRerankerRequest.class))).thenReturn(clovaResponse);

        // when
        MemeRerankerResponse response = memeSearchService.getRerankerMeme(query);

        // then
        then(response).isNotNull();
        then(response.result()).isEqualTo("검색 결과 없음");
        then(response.documents()).isEmpty();
        then(response.suggestedQueries()).containsExactly("추천 쿼리1");
    }

    @Test
    @DisplayName("getRerankerMeme: null 쿼리일 때 FAILURE를 반환하고 API를 호출하지 않는다")
    void getRerankerMeme_returns_failure_when_query_is_null() {
        // when
        MemeRerankerResponse response = memeSearchService.getRerankerMeme(null);

        // then
        then(response).isNotNull();
        then(response.result()).isEmpty();
        then(response.documents()).isEmpty();
        then(response.suggestedQueries()).isEmpty();

        verify(memeRepository, never()).findByFlagOrderByIdDesc(any());
        verify(naverClovaClient, never()).reranker(any());
    }

    @Test
    @DisplayName("getRerankerMeme: 빈 쿼리일 때 FAILURE를 반환하고 API를 호출하지 않는다")
    void getRerankerMeme_returns_failure_when_query_is_blank() {
        // when
        MemeRerankerResponse response = memeSearchService.getRerankerMeme("   ");

        // then
        then(response).isNotNull();
        then(response.result()).isEmpty();
        then(response.documents()).isEmpty();
        then(response.suggestedQueries()).isEmpty();

        verify(memeRepository, never()).findByFlagOrderByIdDesc(any());
        verify(naverClovaClient, never()).reranker(any());
    }

    @Test
    @DisplayName("getRerankerMeme: 후보 밈이 없어도 API 호출을 진행한다")
    void getRerankerMeme_calls_api_even_when_no_candidate_memes() {
        // given
        String query = "무야호";
        List<Meme> candidateMemes = Collections.emptyList();

        ClovaRerankerResponse.Result.Usage usage = new ClovaRerankerResponse.Result.Usage(100, 50, 150);
        ClovaRerankerResponse.Result result = new ClovaRerankerResponse.Result(
            "검색 결과 없음",
            Collections.emptyList(),
            List.of("추천 쿼리1"),
            usage
        );
        ClovaRerankerResponse clovaResponse = new ClovaRerankerResponse(
            new ClovaRerankerResponse.Status("200", "OK"),
            result
        );

        when(memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL)).thenReturn(candidateMemes);
        when(naverClovaClient.reranker(any(ClovaRerankerRequest.class))).thenReturn(clovaResponse);

        // when
        MemeRerankerResponse response = memeSearchService.getRerankerMeme(query);

        // then
        then(response).isNotNull();
        verify(naverClovaClient).reranker(any(ClovaRerankerRequest.class));
    }

    @Test
    @DisplayName("getRerankerMeme: 빈 문자열 쿼리를 거부한다")
    void getRerankerMeme_rejects_empty_string_query() {
        // when
        MemeRerankerResponse response = memeSearchService.getRerankerMeme("");

        // then
        then(response).isNotNull();
        verify(memeRepository, never()).findByFlagOrderByIdDesc(any());
        verify(naverClovaClient, never()).reranker(any());
    }
}
