package spring.memewikibe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Application-wide configuration for HTTP clients and other beans.
 *
 * <p>The RestTemplate bean is configured with appropriate timeouts and error handling
 * to ensure resilience when calling external services (e.g., Naver AI APIs).
 */
@Slf4j
@Configuration
public class AppConfig {

    /**
     * Creates a RestTemplate bean with timeout configuration and logging.
     *
     * <p>Timeout Configuration:
     * <ul>
     *   <li>Connection timeout: 5 seconds - prevents hanging on connection establishment</li>
     *   <li>Read timeout: 30 seconds - allows time for API responses but prevents indefinite waiting</li>
     * </ul>
     *
     * <p>This RestTemplate is used by:
     * <ul>
     *   <li>NaverRagService - AI-powered meme recommendation</li>
     *   <li>NaverCrossEncoderReranker - Search result reranking</li>
     *   <li>NaverQueryRewriter - Query expansion for better search</li>
     * </ul>
     *
     * @param builder the RestTemplateBuilder provided by Spring Boot
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .requestFactory(() -> clientHttpRequestFactory())
            .additionalInterceptors(loggingInterceptor())
            .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        // BufferingClientHttpRequestFactory allows reading the response body multiple times (for logging)
        return new BufferingClientHttpRequestFactory(factory);
    }

    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("RestTemplate Request: {} {}", request.getMethod(), request.getURI());
            var response = execution.execute(request, body);
            log.debug("RestTemplate Response: {} (status: {})",
                request.getURI(),
                response.getStatusCode());
            return response;
        };
    }
}
