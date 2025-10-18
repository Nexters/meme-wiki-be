package spring.memewikibe.external.google;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.external.google.application.ImageGenerationException;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageGeneratorTest {

    @Mock
    private GoogleGenAiClient googleGenAiClient;

    @InjectMocks
    private ImageGenerator imageGenerator;

    private GenerateContentResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Create a mock response with usage metadata
        GenerateContentResponse.UsageMetadata usageMetadata =
            new GenerateContentResponse.UsageMetadata(100, 50, 150, 0);
        mockResponse = new GenerateContentResponse(
            Collections.emptyList(),
            null,
            usageMetadata,
            "gemini-2.5-flash-image-preview",
            "test-response-id"
        );
    }

    @Nested
    @DisplayName("generateImage() tests")
    class GenerateImageTests {

        @Test
        @DisplayName("Should successfully generate image with valid prompt")
        void shouldGenerateImageWithValidPrompt() {
            // given
            String prompt = "A beautiful sunset over mountains";
            when(googleGenAiClient.generateContent(anyString(), anyString(), any(GenerateContentRequest.class)))
                .thenReturn(mockResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImage(prompt);

            // then
            assertThat(response).isNotNull();
            verify(googleGenAiClient).generateContent(
                eq("v1beta"),
                eq("gemini-2.5-flash-image-preview"),
                any(GenerateContentRequest.class)
            );
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void shouldThrowExceptionWhenPromptIsNull() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is blank")
        void shouldThrowExceptionWhenPromptIsBlank() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImage("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is empty")
        void shouldThrowExceptionWhenPromptIsEmpty() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImage(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle response with null usage metadata gracefully")
        void shouldHandleNullUsageMetadataGracefully() {
            // given
            String prompt = "A cat";
            GenerateContentResponse responseWithNullMetadata = new GenerateContentResponse(
                Collections.emptyList(),
                null,
                null,
                "gemini-2.5-flash-image-preview",
                "test-response-id"
            );
            when(googleGenAiClient.generateContent(anyString(), anyString(), any(GenerateContentRequest.class)))
                .thenReturn(responseWithNullMetadata);

            // when
            GenerateContentResponse response = imageGenerator.generateImage(prompt);

            // then
            assertThat(response).isNotNull();
            assertThat(response.usageMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("generateImageCombine() tests")
    class GenerateImageCombineTests {

        @Test
        @DisplayName("Should successfully generate image with valid prompt and images")
        void shouldGenerateImageWithValidPromptAndImages() {
            // given
            String prompt = "Modify this image";
            Base64Image image = new Base64Image("image/png", "base64EncodedData");
            List<Base64Image> images = List.of(image);

            when(googleGenAiClient.generateContent(anyString(), anyString(), any(GenerateContentRequest.class)))
                .thenReturn(mockResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, images);

            // then
            assertThat(response).isNotNull();
            verify(googleGenAiClient).generateContent(
                eq("v1beta"),
                eq("gemini-2.5-flash-image-preview"),
                any(GenerateContentRequest.class)
            );
        }

        @Test
        @DisplayName("Should successfully generate image with multiple images")
        void shouldGenerateImageWithMultipleImages() {
            // given
            String prompt = "Combine these images";
            Base64Image image1 = new Base64Image("image/png", "base64Data1");
            Base64Image image2 = new Base64Image("image/jpeg", "base64Data2");
            List<Base64Image> images = List.of(image1, image2);

            when(googleGenAiClient.generateContent(anyString(), anyString(), any(GenerateContentRequest.class)))
                .thenReturn(mockResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, images);

            // then
            assertThat(response).isNotNull();
            verify(googleGenAiClient).generateContent(anyString(), anyString(), any(GenerateContentRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void shouldThrowExceptionWhenPromptIsNull() {
            // given
            Base64Image image = new Base64Image("image/png", "base64Data");
            List<Base64Image> images = List.of(image);

            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageCombine(null, images))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is blank")
        void shouldThrowExceptionWhenPromptIsBlank() {
            // given
            Base64Image image = new Base64Image("image/png", "base64Data");
            List<Base64Image> images = List.of(image);

            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageCombine("  ", images))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when images list is null")
        void shouldThrowExceptionWhenImagesIsNull() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageCombine("A prompt", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Images list cannot be null or empty");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when images list is empty")
        void shouldThrowExceptionWhenImagesIsEmpty() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageCombine("A prompt", Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Images list cannot be null or empty");

            verify(googleGenAiClient, never()).generateContent(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("generateImageWithExistingImage() tests")
    class GenerateImageWithExistingImageTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void shouldThrowExceptionWhenPromptIsNull() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage(null, "https://example.com/image.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is blank")
        void shouldThrowExceptionWhenPromptIsBlank() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage("   ", "https://example.com/image.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural language prompt cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when image URL is null")
        void shouldThrowExceptionWhenImageUrlIsNull() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage("Modify this", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image URL cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when image URL is blank")
        void shouldThrowExceptionWhenImageUrlIsBlank() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage("Modify this", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image URL cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when image URL is empty")
        void shouldThrowExceptionWhenImageUrlIsEmpty() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage("Modify this", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image URL cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw ImageGenerationException when image download fails")
        void shouldThrowImageGenerationExceptionWhenDownloadFails() {
            // when & then
            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage("Modify this", "https://invalid-url-that-will-fail.com/image.png"))
                .isInstanceOf(ImageGenerationException.class)
                .hasMessageContaining("Failed to download or process image from URL");
        }
    }
}
