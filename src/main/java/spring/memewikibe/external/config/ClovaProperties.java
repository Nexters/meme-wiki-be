package spring.memewikibe.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clova")
public record ClovaProperties(
    String apiKey,
    String baseUrl,
    int connectTimeout,
    int readTimeout
) {

}
