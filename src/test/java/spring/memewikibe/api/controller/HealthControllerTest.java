package spring.memewikibe.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController())
            .build();
    }

    @Test
    @DisplayName("헬스 체크 엔드포인트는 성공 응답을 반환한다")
    void health_ReturnsSuccessResponse() throws Exception {
        // when & then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"))
            .andExpect(jsonPath("$.success").value("healthy"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("헬스 체크 엔드포인트는 GET 메서드로 접근 가능하다")
    void health_IsAccessibleViaGetMethod() throws Exception {
        // when & then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("헬스 체크 응답은 ApiResponse 표준 형식을 따른다")
    void health_FollowsApiResponseFormat() throws Exception {
        // when & then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").exists())
            .andExpect(jsonPath("$.success").exists());
    }
}
