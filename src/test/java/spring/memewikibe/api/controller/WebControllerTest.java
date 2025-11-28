package spring.memewikibe.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import spring.memewikibe.annotation.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link WebController}.
 * <p>
 * Verifies that the web controller correctly serves Thymeleaf templates
 * for the application's landing page.
 */
@IntegrationTest
@AutoConfigureMockMvc
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 메인_페이지를_정상적으로_렌더링한다() throws Exception {
        // when & then
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(forwardedUrl(null)); // Thymeleaf will resolve the actual template
    }

    @Test
    void 메인_페이지_요청은_HTML을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
