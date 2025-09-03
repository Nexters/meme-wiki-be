package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.MemeDoc;
import spring.memewikibe.external.NaverClovaClient;
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

    @Value("${clova.api-key}")
    private String clovaApiKey;

    public List<MemeDoc> getRerankerMeme(String query) {
        List<Meme> candidateMemes = memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL);
        List<MemeDoc> memeDocs = candidateMemes.stream()
            .map(MemeDoc::from)
            .toList();
        String authorization = "Bearer " + clovaApiKey;
        ClovaRerankerResponse reranker = naverClovaClient.reranker(authorization, new ClovaRerankerRequest(memeDocs, query));
        if (reranker.status().code().equals("20000")) {
            if (reranker.result().citedDocuments().isEmpty()) {
                log.info("result: " + reranker.result().result() + "suggestedQueries" + reranker.result().suggestedQueries().toString());
                throw new IllegalStateException();
            }
            return reranker.result().citedDocuments();
        } else {
            log.error("클로바 에러");
            throw new IllegalStateException("클로바 에러");
        }
    }
}
