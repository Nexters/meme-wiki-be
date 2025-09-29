package spring.memewikibe.external.google.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.genai")
public record GoogleGenAiProperties(
    String apiKey,
    String baseUrl
) {
}
