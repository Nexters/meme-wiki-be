package spring.memewikibe.external.google.application;

import com.google.genai.types.GenerateContentParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

@Slf4j
@Component
public class ImageGenerator {

    private static final String DEFAULT_VERSION = "v1beta";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash-image-preview";
    private final GoogleGenAiClient googleGenAiClient;

    public ImageGenerator(GoogleGenAiClient googleGenAiClient) {
        this.googleGenAiClient = googleGenAiClient;
    }

    public GenerateContentResponse generateImage(String naturalLanguage) {
        GenerateContentRequest request = GenerateContentRequest.textOnly(naturalLanguage);

        GenerateContentResponse response = googleGenAiClient.generateContent(
            DEFAULT_VERSION,
            DEFAULT_MODEL,
            request
        );
        log.info("====token usage====  promptTokenCount: {}, candidatesTokenCount: {}, cachedContentTokenCount: {}",
            response.usageMetadata().promptTokenCount(),
            response.usageMetadata().candidatesTokenCount(),
            response.usageMetadata().cachedContentTokenCount());
        return response;
    }
}
