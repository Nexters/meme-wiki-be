package spring.memewikibe.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import spring.memewikibe.external.request.ClovaRerankerRequest;
import spring.memewikibe.external.response.ClovaRerankerResponse;

@FeignClient(name = "ClovaClient", url = "https://clovastudio.stream.ntruss.com/")
public interface NaverClovaClient {

    @PostMapping("/v1/api-tools/reranker")
    ClovaRerankerResponse reranker(
        @RequestHeader("Authorization") String authorization,
        @RequestBody ClovaRerankerRequest request
    );

}
