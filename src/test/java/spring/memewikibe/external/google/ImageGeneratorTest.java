package spring.memewikibe.external.google;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageGeneratorTest {

    @Mock
    private GoogleGenAiClient googleGenAiClient;

    @InjectMocks
    private ImageGenerator imageGenerator;

    @Nested
    @DisplayName("generateImage")
    class GenerateImageTest {

        @Test
        @DisplayName("Should successfully generate image with text prompt")
        void shouldGenerateImageWithTextPrompt() {
            // given
            String naturalLanguage = "A cute red panda";
            GenerateContentResponse expectedResponse = createMockResponse();
            given(googleGenAiClient.generateContent(any(), any(), any()))
                .willReturn(expectedResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImage(naturalLanguage);

            // then
            assertThat(response).isNotNull();
            verify(googleGenAiClient).generateContent(
                eq("v1beta"),
                eq("gemini-2.5-flash-image-preview"),
                any(GenerateContentRequest.class)
            );
        }

        @Test
        @DisplayName("Should handle response with null usageMetadata gracefully")
        void shouldHandleNullUsageMetadataGracefully() {
            // given
            String naturalLanguage = "A cute red panda";
            GenerateContentResponse responseWithNullMetadata = new GenerateContentResponse(
                List.of(),
                null,
                null,
                "model-version",
                "response-id"
            );
            given(googleGenAiClient.generateContent(any(), any(), any()))
                .willReturn(responseWithNullMetadata);

            // when
            GenerateContentResponse response = imageGenerator.generateImage(naturalLanguage);

            // then
            assertThat(response).isNotNull();
            assertThat(response.usageMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("generateImageWithExistingImage")
    class GenerateImageWithExistingImageTest {

        @Test
        @DisplayName("Should throw RuntimeException when image URL is invalid")
        void shouldThrowExceptionWhenImageUrlIsInvalid() {
            // given
            String naturalLanguage = "Modify this image";
            String invalidImageUrl = "invalid-url";

            // when & then
            assertThatThrownBy(() ->
                imageGenerator.generateImageWithExistingImage(naturalLanguage, invalidImageUrl)
            )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to generate image with existing image");
        }
    }

    @Nested
    @DisplayName("generateImageCombine")
    class GenerateImageCombineTest {

        @Test
        @DisplayName("Should successfully generate image with multiple images")
        void shouldGenerateImageWithMultipleImages() {
            // given
            String naturalLanguage = "Combine these images";
            List<Base64Image> images = List.of(
                new Base64Image("image/png", "base64EncodedData1"),
                new Base64Image("image/jpeg", "base64EncodedData2")
            );
            GenerateContentResponse expectedResponse = createMockResponse();
            given(googleGenAiClient.generateContent(any(), any(), any()))
                .willReturn(expectedResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImageCombine(naturalLanguage, images);

            // then
            assertThat(response).isNotNull();
            verify(googleGenAiClient).generateContent(
                eq("v1beta"),
                eq("gemini-2.5-flash-image-preview"),
                any(GenerateContentRequest.class)
            );
        }

        @Test
        @DisplayName("Should successfully generate image with single image")
        void shouldGenerateImageWithSingleImage() {
            // given
            String naturalLanguage = "Modify this image";
            List<Base64Image> images = List.of(
                new Base64Image("image/png", "base64EncodedData")
            );
            GenerateContentResponse expectedResponse = createMockResponse();
            given(googleGenAiClient.generateContent(any(), any(), any()))
                .willReturn(expectedResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImageCombine(naturalLanguage, images);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty images list")
        void shouldHandleEmptyImagesList() {
            // given
            String naturalLanguage = "Generate something";
            List<Base64Image> images = List.of();
            GenerateContentResponse expectedResponse = createMockResponse();
            given(googleGenAiClient.generateContent(any(), any(), any()))
                .willReturn(expectedResponse);

            // when
            GenerateContentResponse response = imageGenerator.generateImageCombine(naturalLanguage, images);

            // then
            assertThat(response).isNotNull();
        }
    }

    private GenerateContentResponse createMockResponse() {
        GenerateContentResponse.UsageMetadata usageMetadata =
            new GenerateContentResponse.UsageMetadata(100, 50, 150, 0);

        return new GenerateContentResponse(
            List.of(),
            null,
            usageMetadata,
            "gemini-2.5-flash-image-preview",
            "test-response-id"
        );
    }
}
