package spring.memewikibe.external.google.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import spring.memewikibe.external.google.client.GoogleGenAiClient;

import java.time.Duration;

@Configuration
public class GoogleGenAiClientConfig {

    private final GoogleGenAiProperties properties;

    public GoogleGenAiClientConfig(GoogleGenAiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public GoogleGenAiClient googleGenAiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        String urlBuilder = properties.baseUrl();
        RestClient restClient = RestClient.builder()
            .baseUrl(urlBuilder)
            .defaultHeader("x-goog-api-key", properties.apiKey())
            .requestFactory(factory)
            .build();

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(GoogleGenAiClient.class);
    }
}
