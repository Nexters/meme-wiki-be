package spring.memewikibe.external.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import spring.memewikibe.external.NaverClovaClient;
import spring.memewikibe.external.exception.ClovaApiException;
import spring.memewikibe.external.response.ClovaErrorResponse;

@Slf4j
@Configuration
public class ClovaClientConfig {

    private final ClovaProperties clovaProperties;
    private final ObjectMapper objectMapper;

    public ClovaClientConfig(ClovaProperties clovaProperties, ObjectMapper objectMapper) {
        this.clovaProperties = clovaProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public NaverClovaClient naverClovaClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(clovaProperties.connectTimeout());
        factory.setReadTimeout(clovaProperties.readTimeout());

        RestClient restClient = RestClient.builder()
            .baseUrl(clovaProperties.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + clovaProperties.apiKey())
            .requestFactory(factory)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                try {
                    String responseBody = new String(response.getBody().readAllBytes());
                    log.error("Clova API Error - Status: {}, Body: {}", response.getStatusCode(), responseBody);

                    ClovaErrorResponse errorResponse = objectMapper.readValue(responseBody, ClovaErrorResponse.class);
                    String clovaCode = errorResponse.status().code();
                    String clovaMessage = errorResponse.status().message();

                    throw ClovaApiException.of(Integer.parseInt(clovaCode), clovaMessage);

                } catch (ClovaApiException e) {
                    throw e; // 이미 파싱된 Clova 에러는 그대로 전파
                } catch (Exception e) {
                    log.error("Failed to parse Clova error response", e);
                    throw ClovaApiException.of(-1, "외부 서비스 에러");
                }
            })
            .build();

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(NaverClovaClient.class);
    }
}
