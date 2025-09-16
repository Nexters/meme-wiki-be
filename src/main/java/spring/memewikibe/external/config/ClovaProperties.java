package spring.memewikibe.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "clova")
public record ClovaProperties(
    String apiKey,
    String baseUrl,
    int connectTimeout,
    int readTimeout
) {

}
