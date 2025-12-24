package spring.memewikibe.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;
import spring.memewikibe.support.response.ApiResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UnitTest
class ControllerAdviceTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new ControllerAdvice())
            .build();
    }

    @Test
    @DisplayName("Validation 실패 시 필드별 에러 메시지를 반환한다")
    void handleValidationException() throws Exception {
        // given
        TestRequest invalidRequest = new TestRequest("");

        // when & then
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E400"))
            .andExpect(jsonPath("$.error.data.name").exists());
    }

    @Test
    @DisplayName("잘못된 JSON 형식 요청 시 적절한 에러 메시지를 반환한다")
    void handleHttpMessageNotReadable() throws Exception {
        // given
        String malformedJson = "{invalid json}";

        // when & then
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E400"));
    }

    @Test
    @DisplayName("MemeWikiApplicationException 발생 시 적절한 에러 응답을 반환한다")
    void handleCustomException() throws Exception {
        // when & then
        mockMvc.perform(post("/test/custom-error")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E404"))
            .andExpect(jsonPath("$.error.message").value("존재하지 않는 밈입니다."));
    }

    @Test
    @DisplayName("예상치 못한 예외 발생 시 기본 에러 응답을 반환한다")
    void handleUnexpectedException() throws Exception {
        // when & then
        mockMvc.perform(post("/test/unexpected-error")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E500"));
    }

    // Test controller for exception handling tests
    @RestController
    static class TestController {

        @PostMapping("/test/validate")
        public ApiResponse<String> validate(@Valid @RequestBody TestRequest request) {
            return ApiResponse.success("OK");
        }

        @PostMapping("/test/custom-error")
        public ApiResponse<String> customError() {
            throw new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND);
        }

        @PostMapping("/test/unexpected-error")
        public ApiResponse<String> unexpectedError() {
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    record TestRequest(@NotBlank(message = "이름은 필수입니다.") String name) {}
}
