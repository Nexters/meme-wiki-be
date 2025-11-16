package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.annotation.IntegrationTest;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.api.controller.image.response.GeneratedImagesResponse;
import spring.memewikibe.common.util.ImageUtils;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@IntegrationTest
class ImageEditServiceTest {

    @Autowired
    private ImageEditService imageEditService;

    @Autowired
    private MemeRepository memeRepository;

    @MockitoBean
    private ImageGenerator imageGenerator;

    @AfterEach
    void tearDown() {
        memeRepository.deleteAllInBatch();
        reset(imageGenerator);
    }

    @Test
    @DisplayName("editMemeImg: 텍스트 프롬프트만으로 밈 이미지 편집 성공")
    void editMemeImg_withTextOnly_success() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        GenerateContentResponse mockResponse = createMockResponse(
            List.of(new Base64Image("image/png", "base64data")),
            List.of("Generated image")
        );
        when(imageGenerator.generateImageWithExistingImage(anyString(), anyString()))
            .thenReturn(mockResponse);

        // when
        GeneratedImagesResponse response = imageEditService.editMemeImg("Add funny text", meme.getId(), null);

        // then
        then(response).isNotNull();
        then(response.images()).hasSize(1);
        then(response.text()).hasSize(1);
        verify(imageGenerator).generateImageWithExistingImage("Add funny text", meme.getImgUrl());
    }

    @Test
    @DisplayName("editMemeImg: 사용자 이미지와 함께 밈 이미지 편집 성공")
    void editMemeImg_withUserImage_success() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        MultipartFile userImage = new MockMultipartFile(
            "file",
            "user.jpg",
            "image/jpeg",
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00}
        );

        GenerateContentResponse mockResponse = createMockResponse(
            List.of(new Base64Image("image/png", "combinedBase64data")),
            List.of("Combined image")
        );
        when(imageGenerator.generateImageCombine(anyString(), anyList()))
            .thenReturn(mockResponse);

        // when & then
        try (MockedStatic<ImageUtils> mockedImageUtils = mockStatic(ImageUtils.class)) {
            mockedImageUtils.when(() -> ImageUtils.downloadBytes(anyString()))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            mockedImageUtils.when(() -> ImageUtils.detectMimeType(anyString(), any()))
                .thenReturn("image/jpeg");

            GeneratedImagesResponse response = imageEditService.editMemeImg("Combine images", meme.getId(), userImage);

            then(response).isNotNull();
            then(response.images()).hasSize(1);
            then(response.text()).hasSize(1);
            verify(imageGenerator).generateImageCombine(anyString(), anyList());
        }
    }

    @Test
    @DisplayName("editMemeImg: 존재하지 않는 밈 ID로 요청 시 예외 발생")
    void editMemeImg_withNonExistentMemeId_throwsException() {
        // given
        Long nonExistentId = 99999L;

        // when & then
        thenThrownBy(() -> imageEditService.editMemeImg("prompt", nonExistentId, null))
            .isInstanceOf(MemeWikiApplicationException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MEME_NOT_FOUND);
    }

    @Test
    @DisplayName("editMemeImg: 비정상 밈으로 요청 시 예외 발생")
    void editMemeImg_withAbnormalMeme_throwsException() {
        // given
        Meme abnormalMeme = memeRepository.save(Meme.builder()
            .title("비정상 밈")
            .imgUrl("https://example.com/abnormal.jpg")
            .flag(Meme.Flag.ABNORMAL)
            .build());

        // when & then
        thenThrownBy(() -> imageEditService.editMemeImg("prompt", abnormalMeme.getId(), null))
            .isInstanceOf(MemeWikiApplicationException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MEME_NOT_FOUND);
    }

    @Test
    @DisplayName("editMemeImg: null 프롬프트로 요청 시 예외 발생")
    void editMemeImg_withNullPrompt_throwsException() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when & then
        thenThrownBy(() -> imageEditService.editMemeImg(null, meme.getId(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Prompt cannot be null or empty");
    }

    @Test
    @DisplayName("editMemeImg: 빈 프롬프트로 요청 시 예외 발생")
    void editMemeImg_withEmptyPrompt_throwsException() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when & then
        thenThrownBy(() -> imageEditService.editMemeImg("   ", meme.getId(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Prompt cannot be null or empty");
    }

    @Test
    @DisplayName("editMemeImg: 빈 사용자 이미지 파일은 무시되고 텍스트만으로 처리")
    void editMemeImg_withEmptyUserImageFile_treatAsTextOnly() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        MultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        GenerateContentResponse mockResponse = createMockResponse(
            List.of(new Base64Image("image/png", "base64data")),
            List.of("Generated image")
        );
        when(imageGenerator.generateImageWithExistingImage(anyString(), anyString()))
            .thenReturn(mockResponse);

        // when
        GeneratedImagesResponse response = imageEditService.editMemeImg("prompt", meme.getId(), emptyFile);

        // then
        then(response).isNotNull();
        verify(imageGenerator).generateImageWithExistingImage(anyString(), anyString());
    }

    @Test
    @DisplayName("editMemeImg: 응답에 이미지가 없는 경우 빈 리스트 반환")
    void editMemeImg_withNoImagesInResponse_returnsEmptyList() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        GenerateContentResponse mockResponse = createMockResponse(
            List.of(),
            List.of("No images generated")
        );
        when(imageGenerator.generateImageWithExistingImage(anyString(), anyString()))
            .thenReturn(mockResponse);

        // when
        GeneratedImagesResponse response = imageEditService.editMemeImg("prompt", meme.getId(), null);

        // then
        then(response).isNotNull();
        then(response.images()).isEmpty();
        then(response.text()).hasSize(1);
    }

    @Test
    @DisplayName("editMemeImg: 응답의 candidates가 null인 경우 안전하게 처리")
    void editMemeImg_withNullCandidates_handlesGracefully() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        GenerateContentResponse mockResponse = new GenerateContentResponse(
            null, // null candidates
            null,
            null,
            "model-v1",
            "response-123"
        );
        when(imageGenerator.generateImageWithExistingImage(anyString(), anyString()))
            .thenReturn(mockResponse);

        // when
        GeneratedImagesResponse response = imageEditService.editMemeImg("prompt", meme.getId(), null);

        // then
        then(response).isNotNull();
        then(response.images()).isEmpty();
        then(response.text()).isEmpty();
    }

    @Test
    @DisplayName("editMemeImg: 응답에 null candidate가 포함된 경우 건너뛰기")
    void editMemeImg_withNullCandidateInList_skipsNull() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        GenerateContentResponse.Candidate validCandidate = new GenerateContentResponse.Candidate(
            new GenerateContentResponse.Content(
                List.of(new GenerateContentResponse.Part("Valid text", null, null, null)),
                "model"
            ),
            "STOP",
            null,
            0
        );

        List<GenerateContentResponse.Candidate> candidatesWithNulls = new java.util.ArrayList<>();
        candidatesWithNulls.add(null);
        candidatesWithNulls.add(validCandidate);
        candidatesWithNulls.add(null);

        GenerateContentResponse mockResponse = new GenerateContentResponse(
            candidatesWithNulls,
            null,
            null,
            "model-v1",
            "response-123"
        );
        when(imageGenerator.generateImageWithExistingImage(anyString(), anyString()))
            .thenReturn(mockResponse);

        // when
        GeneratedImagesResponse response = imageEditService.editMemeImg("prompt", meme.getId(), null);

        // then
        then(response).isNotNull();
        then(response.text()).hasSize(1);
        then(response.text()).containsExactly("Valid text");
    }

    @Test
    @DisplayName("editMemeImg: 여러 이미지와 텍스트가 포함된 응답 처리")
    void editMemeImg_withMultipleImagesAndTexts_handlesCorrectly() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        GenerateContentResponse mockResponse = createMockResponse(
            List.of(
                new Base64Image("image/png", "image1"),
                new Base64Image("image/jpeg", "image2"),
                new Base64Image("image/webp", "image3")
            ),
            List.of("Text 1", "Text 2", "Text 3")
        );
        when(imageGenerator.generateImageWithExistingImage(anyString(), anyString()))
            .thenReturn(mockResponse);

        // when
        GeneratedImagesResponse response = imageEditService.editMemeImg("prompt", meme.getId(), null);

        // then
        then(response).isNotNull();
        then(response.images()).hasSize(3);
        then(response.text()).hasSize(3);
        then(response.images()).extracting(Base64Image::mimeType)
            .containsExactly("image/png", "image/jpeg", "image/webp");
        then(response.text()).containsExactly("Text 1", "Text 2", "Text 3");
    }

    private GenerateContentResponse createMockResponse(List<Base64Image> images, List<String> texts) {
        List<GenerateContentResponse.Part> parts = new java.util.ArrayList<>();

        for (Base64Image image : images) {
            parts.add(new GenerateContentResponse.Part(
                null,
                new GenerateContentResponse.InlineData(image.mimeType(), image.data()),
                null,
                null
            ));
        }

        for (String text : texts) {
            parts.add(new GenerateContentResponse.Part(text, null, null, null));
        }

        GenerateContentResponse.Candidate candidate = new GenerateContentResponse.Candidate(
            new GenerateContentResponse.Content(parts, "model"),
            "STOP",
            null,
            0
        );

        return new GenerateContentResponse(
            List.of(candidate),
            null,
            null,
            "model-v1",
            "response-123"
        );
    }
}
