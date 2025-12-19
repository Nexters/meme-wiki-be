package spring.memewikibe.api.controller.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.api.controller.image.response.GeneratedImagesResponse;
import spring.memewikibe.application.ImageEditService;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageEditService imageEditService;

    @Test
    @DisplayName("POST /api/images/edit/meme/{memeId}: 프롬프트만으로 밈 이미지 편집 성공")
    void editWithMemeMultipart_withPromptOnly_success() throws Exception {
        // given
        Long memeId = 1L;
        String prompt = "Add funny text to the meme";

        GeneratedImagesResponse mockResponse = new GeneratedImagesResponse(
            List.of(new Base64Image("image/png", "base64encodeddata")),
            List.of("Generated successfully")
        );

        when(imageEditService.editMemeImg(eq(prompt), eq(memeId), isNull()))
            .thenReturn(mockResponse);

        MockMultipartFile promptPart = new MockMultipartFile(
            "prompt",
            "",
            MediaType.TEXT_PLAIN_VALUE,
            prompt.getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/images/edit/meme/{memeId}", memeId)
                .file(promptPart))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"))
            .andExpect(jsonPath("$.success.images").isArray())
            .andExpect(jsonPath("$.success.images[0].mimeType").value("image/png"))
            .andExpect(jsonPath("$.success.images[0].data").value("base64encodeddata"))
            .andExpect(jsonPath("$.success.text").isArray())
            .andExpect(jsonPath("$.success.text[0]").value("Generated successfully"));

        verify(imageEditService).editMemeImg(prompt, memeId, null);
    }

    @Test
    @DisplayName("POST /api/images/edit/meme/{memeId}: 프롬프트와 이미지로 밈 이미지 편집 성공")
    void editWithMemeMultipart_withPromptAndImage_success() throws Exception {
        // given
        Long memeId = 1L;
        String prompt = "Combine these images";

        GeneratedImagesResponse mockResponse = new GeneratedImagesResponse(
            List.of(new Base64Image("image/png", "combinedImageData")),
            List.of("Images combined")
        );

        when(imageEditService.editMemeImg(eq(prompt), eq(memeId), any()))
            .thenReturn(mockResponse);

        MockMultipartFile promptPart = new MockMultipartFile(
            "prompt",
            "",
            MediaType.TEXT_PLAIN_VALUE,
            prompt.getBytes()
        );

        MockMultipartFile imagePart = new MockMultipartFile(
            "image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        // when & then
        mockMvc.perform(multipart("/api/images/edit/meme/{memeId}", memeId)
                .file(promptPart)
                .file(imagePart)
)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"))
            .andExpect(jsonPath("$.success.images").isArray())
            .andExpect(jsonPath("$.success.images[0].mimeType").value("image/png"))
            .andExpect(jsonPath("$.success.images[0].data").value("combinedImageData"))
            .andExpect(jsonPath("$.success.text").isArray())
            .andExpect(jsonPath("$.success.text[0]").value("Images combined"));

        verify(imageEditService).editMemeImg(eq(prompt), eq(memeId), any());
    }

    @Test
    @DisplayName("POST /api/images/edit/meme/{memeId}: 존재하지 않는 밈 ID로 요청 시 404 에러")
    void editWithMemeMultipart_withNonExistentMemeId_returns404() throws Exception {
        // given
        Long nonExistentMemeId = 99999L;
        String prompt = "Edit this meme";

        when(imageEditService.editMemeImg(eq(prompt), eq(nonExistentMemeId), isNull()))
            .thenThrow(new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));

        MockMultipartFile promptPart = new MockMultipartFile(
            "prompt",
            "",
            MediaType.TEXT_PLAIN_VALUE,
            prompt.getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/images/edit/meme/{memeId}", nonExistentMemeId)
                .file(promptPart)
)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E404"));

        verify(imageEditService).editMemeImg(prompt, nonExistentMemeId, null);
    }

    @Test
    @DisplayName("POST /api/images/edit/meme/{memeId}: 빈 응답 처리")
    void editWithMemeMultipart_withEmptyResponse_success() throws Exception {
        // given
        Long memeId = 1L;
        String prompt = "Generate image";

        GeneratedImagesResponse emptyResponse = new GeneratedImagesResponse(
            List.of(),
            List.of()
        );

        when(imageEditService.editMemeImg(eq(prompt), eq(memeId), isNull()))
            .thenReturn(emptyResponse);

        MockMultipartFile promptPart = new MockMultipartFile(
            "prompt",
            "",
            MediaType.TEXT_PLAIN_VALUE,
            prompt.getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/images/edit/meme/{memeId}", memeId)
                .file(promptPart)
)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"))
            .andExpect(jsonPath("$.success.images").isEmpty())
            .andExpect(jsonPath("$.success.text").isEmpty());

        verify(imageEditService).editMemeImg(prompt, memeId, null);
    }

    @Test
    @DisplayName("POST /api/images/edit/meme/{memeId}: 여러 이미지와 텍스트 응답 처리")
    void editWithMemeMultipart_withMultipleImagesAndTexts_success() throws Exception {
        // given
        Long memeId = 1L;
        String prompt = "Generate multiple variations";

        GeneratedImagesResponse multiResponse = new GeneratedImagesResponse(
            List.of(
                new Base64Image("image/png", "image1data"),
                new Base64Image("image/jpeg", "image2data"),
                new Base64Image("image/webp", "image3data")
            ),
            List.of("Variation 1", "Variation 2", "Variation 3")
        );

        when(imageEditService.editMemeImg(eq(prompt), eq(memeId), isNull()))
            .thenReturn(multiResponse);

        MockMultipartFile promptPart = new MockMultipartFile(
            "prompt",
            "",
            MediaType.TEXT_PLAIN_VALUE,
            prompt.getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/images/edit/meme/{memeId}", memeId)
                .file(promptPart)
)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"))
            .andExpect(jsonPath("$.success.images").isArray())
            .andExpect(jsonPath("$.success.images.length()").value(3))
            .andExpect(jsonPath("$.success.images[0].mimeType").value("image/png"))
            .andExpect(jsonPath("$.success.images[1].mimeType").value("image/jpeg"))
            .andExpect(jsonPath("$.success.images[2].mimeType").value("image/webp"))
            .andExpect(jsonPath("$.success.text.length()").value(3))
            .andExpect(jsonPath("$.success.text[0]").value("Variation 1"))
            .andExpect(jsonPath("$.success.text[1]").value("Variation 2"))
            .andExpect(jsonPath("$.success.text[2]").value("Variation 3"));

        verify(imageEditService).editMemeImg(prompt, memeId, null);
    }
}
