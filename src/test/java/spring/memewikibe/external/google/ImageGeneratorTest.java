package spring.memewikibe.external.google;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.common.util.ImageUtils;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.GoogleGenAiClient;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageGeneratorTest {
    private GoogleGenAiClient googleGenAiClient;
    private ImageGenerator imageGenerator;

    @BeforeEach
    void setUp() {
        googleGenAiClient = mock(GoogleGenAiClient.class);
        imageGenerator = new ImageGenerator(googleGenAiClient);
    }

    @Test
    @DisplayName("generateImage: 텍스트로 이미지 생성 성공")
    void generateImage_withText_success() {
        // given
        String prompt = "A happy cat";
        GenerateContentResponse mockResponse = createMockResponse();
        when(googleGenAiClient.generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), any()))
            .thenReturn(mockResponse);

        // when
        GenerateContentResponse response = imageGenerator.generateImage(prompt);

        // then
        assertThat(response).isNotNull();
        assertThat(response.modelVersion()).isEqualTo("model-v1");

        ArgumentCaptor<GenerateContentRequest> requestCaptor = ArgumentCaptor.forClass(GenerateContentRequest.class);
        verify(googleGenAiClient).generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), requestCaptor.capture());

        GenerateContentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.contents()).hasSize(1);
        assertThat(capturedRequest.contents().get(0).parts()).hasSize(1);
        assertThat(capturedRequest.contents().get(0).parts().get(0).text()).isEqualTo(prompt);
    }

    @Test
    @DisplayName("generateImageWithExistingImage: 기존 이미지 URL로 이미지 생성 성공")
    void generateImageWithExistingImage_withValidUrl_success() {
        // given
        String prompt = "Make it funny";
        String imageUrl = "https://example.com/test.jpg";
        byte[] mockImageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        String mimeType = "image/jpeg";

        GenerateContentResponse mockResponse = createMockResponse();
        when(googleGenAiClient.generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), any()))
            .thenReturn(mockResponse);

        // when & then
        try (MockedStatic<ImageUtils> mockedImageUtils = mockStatic(ImageUtils.class)) {
            mockedImageUtils.when(() -> ImageUtils.downloadBytes(imageUrl))
                .thenReturn(mockImageBytes);
            mockedImageUtils.when(() -> ImageUtils.detectMimeType(imageUrl, mockImageBytes))
                .thenReturn(mimeType);

            GenerateContentResponse response = imageGenerator.generateImageWithExistingImage(prompt, imageUrl);

            assertThat(response).isNotNull();
            assertThat(response.modelVersion()).isEqualTo("model-v1");

            ArgumentCaptor<GenerateContentRequest> requestCaptor = ArgumentCaptor.forClass(GenerateContentRequest.class);
            verify(googleGenAiClient).generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), requestCaptor.capture());

            GenerateContentRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.contents()).hasSize(1);
            assertThat(capturedRequest.contents().get(0).parts()).hasSize(2);

            // First part should be image
            GenerateContentRequest.Part imagePart = capturedRequest.contents().get(0).parts().get(0);
            assertThat(imagePart.text()).isNull();
            assertThat(imagePart.inlineData()).isNotNull();
            assertThat(imagePart.inlineData().mimeType()).isEqualTo(mimeType);

            // Second part should be text
            GenerateContentRequest.Part textPart = capturedRequest.contents().get(0).parts().get(1);
            assertThat(textPart.text()).isEqualTo(prompt);
            assertThat(textPart.inlineData()).isNull();
        }
    }

    @Test
    @DisplayName("generateImageWithExistingImage: 이미지 다운로드 실패 시 예외 발생")
    void generateImageWithExistingImage_whenDownloadFails_throwsException() {
        // given
        String prompt = "Make it funny";
        String imageUrl = "https://example.com/invalid.jpg";

        // when & then
        try (MockedStatic<ImageUtils> mockedImageUtils = mockStatic(ImageUtils.class)) {
            mockedImageUtils.when(() -> ImageUtils.downloadBytes(imageUrl))
                .thenThrow(new IOException("Download failed"));

            assertThatThrownBy(() -> imageGenerator.generateImageWithExistingImage(prompt, imageUrl))
                .isInstanceOf(MemeWikiApplicationException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.DEFAULT_ERROR);
        }
    }

    @Test
    @DisplayName("generateImageCombine: 여러 이미지와 텍스트로 이미지 생성 성공")
    void generateImageCombine_withMultipleImages_success() {
        // given
        String prompt = "Combine these";
        List<Base64Image> images = List.of(
            new Base64Image("image/png", "base64data1"),
            new Base64Image("image/jpeg", "base64data2"),
            new Base64Image("image/webp", "base64data3")
        );

        GenerateContentResponse mockResponse = createMockResponse();
        when(googleGenAiClient.generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), any()))
            .thenReturn(mockResponse);

        // when
        GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, images);

        // then
        assertThat(response).isNotNull();
        assertThat(response.modelVersion()).isEqualTo("model-v1");

        ArgumentCaptor<GenerateContentRequest> requestCaptor = ArgumentCaptor.forClass(GenerateContentRequest.class);
        verify(googleGenAiClient).generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), requestCaptor.capture());

        GenerateContentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.contents()).hasSize(1);
        assertThat(capturedRequest.contents().get(0).parts()).hasSize(4); // 3 images + 1 text

        // Verify image parts
        for (int i = 0; i < 3; i++) {
            GenerateContentRequest.Part part = capturedRequest.contents().get(0).parts().get(i);
            assertThat(part.text()).isNull();
            assertThat(part.inlineData()).isNotNull();
            assertThat(part.inlineData().mimeType()).isEqualTo(images.get(i).mimeType());
            assertThat(part.inlineData().data()).isEqualTo(images.get(i).data());
        }

        // Verify text part
        GenerateContentRequest.Part textPart = capturedRequest.contents().get(0).parts().get(3);
        assertThat(textPart.text()).isEqualTo(prompt);
        assertThat(textPart.inlineData()).isNull();
    }

    @Test
    @DisplayName("generateImageCombine: 단일 이미지와 텍스트로 이미지 생성 성공")
    void generateImageCombine_withSingleImage_success() {
        // given
        String prompt = "Edit this image";
        List<Base64Image> images = List.of(
            new Base64Image("image/png", "singleImageData")
        );

        GenerateContentResponse mockResponse = createMockResponse();
        when(googleGenAiClient.generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), any()))
            .thenReturn(mockResponse);

        // when
        GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, images);

        // then
        assertThat(response).isNotNull();

        ArgumentCaptor<GenerateContentRequest> requestCaptor = ArgumentCaptor.forClass(GenerateContentRequest.class);
        verify(googleGenAiClient).generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), requestCaptor.capture());

        GenerateContentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.contents()).hasSize(1);
        assertThat(capturedRequest.contents().get(0).parts()).hasSize(2); // 1 image + 1 text
    }

    @Test
    @DisplayName("generateImageCombine: 빈 이미지 리스트로 요청 시 텍스트만 포함")
    void generateImageCombine_withEmptyImageList_containsOnlyText() {
        // given
        String prompt = "Just text";
        List<Base64Image> emptyImages = List.of();

        GenerateContentResponse mockResponse = createMockResponse();
        when(googleGenAiClient.generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), any()))
            .thenReturn(mockResponse);

        // when
        GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, emptyImages);

        // then
        assertThat(response).isNotNull();

        ArgumentCaptor<GenerateContentRequest> requestCaptor = ArgumentCaptor.forClass(GenerateContentRequest.class);
        verify(googleGenAiClient).generateContent(eq("v1beta"), eq("gemini-2.5-flash-image-preview"), requestCaptor.capture());

        GenerateContentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.contents()).hasSize(1);
        assertThat(capturedRequest.contents().get(0).parts()).hasSize(1); // Only text

        GenerateContentRequest.Part textPart = capturedRequest.contents().get(0).parts().get(0);
        assertThat(textPart.text()).isEqualTo(prompt);
        assertThat(textPart.inlineData()).isNull();
    }

    private GenerateContentResponse createMockResponse() {
        GenerateContentResponse.UsageMetadata usageMetadata =
            new GenerateContentResponse.UsageMetadata(100, 50, 150, 0);

        GenerateContentResponse.Part textPart =
            new GenerateContentResponse.Part("Generated text", null, null, null);

        GenerateContentResponse.Content content =
            new GenerateContentResponse.Content(List.of(textPart), "model");

        GenerateContentResponse.Candidate candidate =
            new GenerateContentResponse.Candidate(content, "STOP", null, 0);

        return new GenerateContentResponse(
            List.of(candidate),
            null,
            usageMetadata,
            "model-v1",
            "response-123"
        );
    }
}
