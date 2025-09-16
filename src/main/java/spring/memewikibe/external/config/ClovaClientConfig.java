package spring.memewikibe.external.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import spring.memewikibe.external.NaverClovaClient;

@Configuration
public class ClovaClientConfig {

    private final ClovaProperties clovaProperties;

    public ClovaClientConfig(ClovaProperties clovaProperties) {
        this.clovaProperties = clovaProperties;
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
            .build();

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(NaverClovaClient.class);
    }
}
