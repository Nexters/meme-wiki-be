package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.meme.response.MemeRerankerResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.NaverClovaClient;
import spring.memewikibe.external.domain.MemeDoc;
import spring.memewikibe.external.request.ClovaRerankerRequest;
import spring.memewikibe.external.response.ClovaRerankerResponse;
import spring.memewikibe.infrastructure.MemeRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemeSearchService {

    private final NaverClovaClient naverClovaClient;
    private final MemeRepository memeRepository;

    public MemeRerankerResponse getRerankerMeme(String query) {
        if (query == null || query.isBlank()) {
            log.warn("Search query is null or blank");
            return MemeRerankerResponse.failure("", List.of(), List.of());
        }

        List<Meme> candidateMemes = memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL);
        if (candidateMemes.isEmpty()) {
            log.warn("No candidate memes available for search | query: '{}'", query);
        }

        List<MemeDoc> memeDocs = candidateMemes.stream()
            .map(MemeDoc::from)
            .toList();
        ClovaRerankerResponse reranker = naverClovaClient.reranker(new ClovaRerankerRequest(memeDocs, query));

        ClovaRerankerResponse.Result result = reranker.result();
        int prompt = result.usage().promptTokens();
        int completion = result.usage().completionTokens();
        int total = result.usage().totalTokens();

        log.info("clova token usage | prompt: {}, completion: {}, total: {}", prompt, completion, total);

        if (reranker.isNotFound()) {
            log.warn("clova reranker is worked but failed to find relevant memes | query: '{}'", query);
            return MemeRerankerResponse.failure(
                result.result(),
                result.citedDocuments(),
                result.suggestedQueries()
            );
        }
        return MemeRerankerResponse.success(
            result.result(),
            result.citedDocuments(),
            result.suggestedQueries()
        );
    }
}
