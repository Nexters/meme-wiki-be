package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.memewikibe.api.controller.meme.response.MemeRerankerResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.ClovaException;
import spring.memewikibe.external.NaverClovaClient;
import spring.memewikibe.external.domain.MemeDoc;
import spring.memewikibe.external.request.ClovaRerankerRequest;
import spring.memewikibe.external.response.ClovaRerankerResponse;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.support.error.ErrorType;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemeSearchService {

    private final NaverClovaClient naverClovaClient;
    private final MemeRepository memeRepository;

    public MemeRerankerResponse getRerankerMeme(String query) {
        List<Meme> candidateMemes = memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL);
        List<MemeDoc> memeDocs = candidateMemes.stream()
            .map(MemeDoc::from)
            .toList();
        ClovaRerankerResponse reranker = naverClovaClient.reranker(new ClovaRerankerRequest(memeDocs, query));

        int prompt = reranker.result().usage().promptTokens();
        int completion = reranker.result().usage().completionTokens();
        int total = reranker.result().usage().totalTokens();

        log.info("clova token usage | prompt: {}, completion: {}, total: {}", prompt, completion, total);

        if (!reranker.isSuccess()) {
            log.error("clova reranker error | code: {}, message: {}", reranker.status().code(), reranker.status().message());
            throw new ClovaException(ErrorType.EXTERNAL_SERVICE_ERROR);
        }

        if (reranker.isNotFound()) {
            log.warn("clova reranker is worked but failed to find relevant memes | query: '{}'", query);
            return MemeRerankerResponse.failure(
                reranker.result().result(),
                List.of(),
                reranker.result().suggestedQueries()
            );
        }
        return MemeRerankerResponse.success(
            reranker.result().result(),
            reranker.result().citedDocuments(),
            reranker.result().suggestedQueries()
        );
    }
}
