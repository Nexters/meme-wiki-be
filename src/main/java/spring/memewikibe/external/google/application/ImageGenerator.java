package spring.memewikibe.external.google.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.common.util.ImageUtils;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.io.IOException;
import java.util.ArrayList;
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
        Base64Image base64Image = convertUrlToBase64Image(imageUrl);
        return generateImageCombine(naturalLanguage, List.of(base64Image));
    }

    public GenerateContentResponse generateImageCombine(String naturalLanguage, List<Base64Image> images) {
        List<GenerateContentRequest.Part> parts = new ArrayList<>();

        for (Base64Image image : images) {
            parts.add(createImagePart(image.mimeType(), image.data()));
        }

        parts.add(createTextPart(naturalLanguage));

        GenerateContentRequest.Content content = new GenerateContentRequest.Content(parts, null);
        GenerateContentRequest request = new GenerateContentRequest(List.of(content));

        GenerateContentResponse response = googleGenAiClient.generateContent(
            DEFAULT_VERSION,
            DEFAULT_MODEL,
            request
        );
        logTokenUsage(response);
        return response;
    }

    private GenerateContentRequest.Part createTextPart(String text) {
        return new GenerateContentRequest.Part(text, null, null, null);
    }

    private GenerateContentRequest.Part createImagePart(String mimeType, String base64Data) {
        GenerateContentRequest.InlineData inlineData = new GenerateContentRequest.InlineData(mimeType, base64Data);
        return new GenerateContentRequest.Part(null, inlineData, null, null);
    }

    private void logTokenUsage(GenerateContentResponse response) {
        log.info("====token usage====  promptTokenCount: {}, candidatesTokenCount: {}, cachedContentTokenCount: {}",
            response.usageMetadata().promptTokenCount(),
            response.usageMetadata().candidatesTokenCount(),
            response.usageMetadata().cachedContentTokenCount());
    }

    private Base64Image convertUrlToBase64Image(String imageUrl) {
        try {
            byte[] imageBytes = ImageUtils.downloadBytes(imageUrl);
            String mimeType = ImageUtils.detectMimeType(imageUrl, imageBytes);
            String base64Data = getEncoder().encodeToString(imageBytes);
            return new Base64Image(mimeType, base64Data);
        } catch (IOException e) {
            log.error("Failed to convert URL to Base64 image: {}", imageUrl, e);
            throw new MemeWikiApplicationException(ErrorType.DEFAULT_ERROR);
        }
    }

}
