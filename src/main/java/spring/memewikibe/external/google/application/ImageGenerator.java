package spring.memewikibe.external.google.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

import java.net.URL;
import java.util.List;

import static java.util.Base64.getEncoder;

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
        logTokenUsage(response);
        return response;
    }

    public GenerateContentResponse generateImageWithExistingImage(String naturalLanguage, String imageUrl) {
        try {
            byte[] imageBytes = downloadBytes(imageUrl);
            String mimeType = detectMimeType(imageUrl, imageBytes);

            GenerateContentRequest.Part textPart = new GenerateContentRequest.Part(
                naturalLanguage,
                null,
                null,
                null
            );

            GenerateContentRequest.Part imagePart = new GenerateContentRequest.Part(
                null,
                new GenerateContentRequest.InlineData(mimeType, getEncoder().encodeToString(imageBytes)),
                null,
                null
            );

            GenerateContentRequest request = new GenerateContentRequest(
                List.of(
                    new GenerateContentRequest.Content(
                        List.of(textPart, imagePart),
                        null
                    )
                )
            );

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
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate image with existing image", e);
        }
    }

    public GenerateContentResponse generateImageWithInlineBase64(String naturalLanguage, String mimeType, String base64Data) {
        GenerateContentRequest.Part textPart = new GenerateContentRequest.Part(
            naturalLanguage,
            null,
            null,
            null
        );

        GenerateContentRequest.Part imagePart = new GenerateContentRequest.Part(
            null,
            new GenerateContentRequest.InlineData(mimeType, base64Data),
            null,
            null
        );

        GenerateContentRequest request = new GenerateContentRequest(
            java.util.List.of(
                new GenerateContentRequest.Content(
                    java.util.List.of(textPart, imagePart),
                    null
                )
            )
        );

        GenerateContentResponse response = googleGenAiClient.generateContent(
            DEFAULT_VERSION,
            DEFAULT_MODEL,
            request
        );
        logTokenUsage(response);
        return response;
    }

    private void logTokenUsage(GenerateContentResponse response) {
        log.info("====token usage====  promptTokenCount: {}, candidatesTokenCount: {}, cachedContentTokenCount: {}",
            response.usageMetadata().promptTokenCount(),
            response.usageMetadata().candidatesTokenCount(),
            response.usageMetadata().cachedContentTokenCount());
    }


    private static byte[] downloadBytes(String url) throws java.io.IOException {
        URL u = new URL(url);
        java.net.URLConnection conn = u.openConnection();
        try (java.io.InputStream in = conn.getInputStream();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static String detectMimeType(String imageUrl, byte[] data) throws java.io.IOException {
        // Try by URL connection header
        try {
            URL u = new URL(imageUrl);
            java.net.URLConnection conn = u.openConnection();
            String ct = conn.getContentType();
            if (StringUtils.hasText(ct)) return ct;
        } catch (Exception ignored) {
        }
        // Try by stream content sniffing
        try (java.io.InputStream is = new java.io.ByteArrayInputStream(data)) {
            String guessed = java.net.URLConnection.guessContentTypeFromStream(is);
            if (StringUtils.hasText(guessed)) return guessed;
        }
        // Fallback by extension
        String lower = imageUrl.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
