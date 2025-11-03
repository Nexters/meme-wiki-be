package spring.memewikibe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Naver AI (Clova Studio) API integration.
 * Used for query rewriting and keyword expansion in search functionality.
 */
@Getter
@Setter
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
     * API endpoint URL for Naver Clova Studio chat completions.
     */
    private String apiEndpoint = "https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003";
}
