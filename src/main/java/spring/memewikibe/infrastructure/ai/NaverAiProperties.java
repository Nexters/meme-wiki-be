package spring.memewikibe.infrastructure.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe configuration properties for Naver AI API integration.
 * Replaces @Value annotations for improved configuration management.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "naver.ai")
public class NaverAiProperties {

    /**
     * Naver AI API key for authentication.
     */
    private String apiKey = "";

    /**
     * Request ID used for tracking API calls.
     */
    private String requestId = "meme-wiki-qrewrite";

    /**
     * API endpoint URL for Naver AI chat completions.
     */
    private String apiEndpoint = "https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003";
}
