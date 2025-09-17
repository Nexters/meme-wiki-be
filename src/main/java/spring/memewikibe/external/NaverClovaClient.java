package spring.memewikibe.external;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import spring.memewikibe.external.request.ClovaRerankerRequest;
import spring.memewikibe.external.response.ClovaRerankerResponse;

@HttpExchange
public interface NaverClovaClient {

    @PostExchange("/v1/api-tools/reranker")
    ClovaRerankerResponse reranker(@RequestBody ClovaRerankerRequest request);

}
