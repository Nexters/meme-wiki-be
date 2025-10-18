package spring.memewikibe.external.google.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.common.util.ImageUtils;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

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

    /**
     * Generates an image based on natural language prompt.
     *
     * @param naturalLanguage the text prompt describing the desired image
     * @return the response containing the generated image
     * @throws IllegalArgumentException if naturalLanguage is null or blank
     */
    public GenerateContentResponse generateImage(String naturalLanguage) {
        validateNaturalLanguage(naturalLanguage);

        GenerateContentRequest request = GenerateContentRequest.textOnly(naturalLanguage);

        GenerateContentResponse response = googleGenAiClient.generateContent(
            DEFAULT_VERSION,
            DEFAULT_MODEL,
            request
        );
        logTokenUsage(response);
        return response;
    }

    /**
     * Generates a modified image based on an existing image and natural language prompt.
     *
     * @param naturalLanguage the text prompt describing the desired modifications
     * @param imageUrl the URL of the existing image to modify
     * @return the response containing the generated image
     * @throws IllegalArgumentException if naturalLanguage is null/blank or imageUrl is null/blank
     * @throws ImageGenerationException if image download or generation fails
     */
    public GenerateContentResponse generateImageWithExistingImage(String naturalLanguage, String imageUrl) {
        validateNaturalLanguage(naturalLanguage);
        validateImageUrl(imageUrl);

        try {
            byte[] imageBytes = ImageUtils.downloadBytes(imageUrl);
            String mimeType = ImageUtils.detectMimeType(imageUrl, imageBytes);
            Base64Image base64Image = new Base64Image(mimeType, getEncoder().encodeToString(imageBytes));

            return generateImageCombine(naturalLanguage, List.of(base64Image));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate image with existing image from URL: {}", imageUrl, e);
            throw new ImageGenerationException("Failed to download or process image from URL: " + imageUrl, e);
        }
    }

    /**
     * Generates an image by combining multiple base64-encoded images with a natural language prompt.
     *
     * @param naturalLanguage the text prompt describing the desired image
     * @param images the list of base64-encoded images to combine
     * @return the response containing the generated image
     * @throws IllegalArgumentException if naturalLanguage is null/blank or images is null/empty
     */
    public GenerateContentResponse generateImageCombine(String naturalLanguage, List<Base64Image> images) {
        validateNaturalLanguage(naturalLanguage);
        validateImages(images);

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
        if (response != null && response.usageMetadata() != null) {
            log.info("====token usage====  promptTokenCount: {}, candidatesTokenCount: {}, cachedContentTokenCount: {}",
                response.usageMetadata().promptTokenCount(),
                response.usageMetadata().candidatesTokenCount(),
                response.usageMetadata().cachedContentTokenCount());
        }
    }

    private void validateNaturalLanguage(String naturalLanguage) {
        if (naturalLanguage == null || naturalLanguage.isBlank()) {
            throw new IllegalArgumentException("Natural language prompt cannot be null or blank");
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Image URL cannot be null or blank");
        }
    }

    private void validateImages(List<Base64Image> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Images list cannot be null or empty");
        }
    }

}
