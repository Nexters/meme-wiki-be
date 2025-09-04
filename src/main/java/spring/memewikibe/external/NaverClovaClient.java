package spring.memewikibe.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import spring.memewikibe.external.config.ClovaFeignConfig;
import spring.memewikibe.external.request.ClovaRerankerRequest;
import spring.memewikibe.external.response.ClovaRerankerResponse;

@FeignClient(
    name = "ClovaClient", 
    url = "https://clovastudio.stream.ntruss.com/",
    configuration = ClovaFeignConfig.class
)
public interface NaverClovaClient {

    @PostMapping("/v1/api-tools/reranker")
    ClovaRerankerResponse reranker(@RequestBody ClovaRerankerRequest request);

}
