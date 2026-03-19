package spring.memewikibe.api.controller.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import spring.memewikibe.annotation.IntegrationTest;
import spring.memewikibe.api.controller.notification.request.NotificationCreateTokenRequest;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationTokenRepository tokenRepository;

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAllInBatch();
    }

    @Test
    void 푸시_알림_토큰_등록에_성공한다() throws Exception {
        // given
        String token = "valid-fcm-token-123";
        NotificationCreateTokenRequest request = new NotificationCreateTokenRequest(token);

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        // verify
        then(tokenRepository.findById(token)).isPresent();
    }

    @Test
    void 동일한_토큰을_여러번_등록해도_성공한다() throws Exception {
        // given
        String token = "duplicate-token";
        NotificationCreateTokenRequest request = new NotificationCreateTokenRequest(token);

        // when & then - first registration
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // when & then - second registration (should also succeed)
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // verify - should only have one token
        then(tokenRepository.findAll()).hasSize(1);
    }

    @Test
    void 빈_토큰으로_등록_시_400_에러를_반환한다() throws Exception {
        // given
        NotificationCreateTokenRequest request = new NotificationCreateTokenRequest("");

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E400"))
            .andExpect(jsonPath("$.error.data.token").value("Token is required"));
    }

    @Test
    void 공백만_있는_토큰으로_등록_시_400_에러를_반환한다() throws Exception {
        // given
        NotificationCreateTokenRequest request = new NotificationCreateTokenRequest("   ");

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E400"));
    }

    @Test
    void null_토큰으로_등록_시_400_에러를_반환한다() throws Exception {
        // given
        String requestBody = "{\"token\": null}";

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E400"));
    }

    @Test
    void 잘못된_JSON_형식으로_요청_시_400_에러를_반환한다() throws Exception {
        // given
        String malformedJson = "{invalid json}";

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultType").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E400"));
    }

    @Test
    void 요청_본문이_없으면_400_에러를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void 매우_긴_토큰도_성공적으로_등록된다() throws Exception {
        // given
        // Firebase Cloud Messaging tokens can be 152+ characters
        String longToken = "a".repeat(200);
        NotificationCreateTokenRequest request = new NotificationCreateTokenRequest(longToken);

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        // verify
        then(tokenRepository.findById(longToken)).isPresent();
    }

    @Test
    void 특수문자가_포함된_토큰도_성공적으로_등록된다() throws Exception {
        // given
        String tokenWithSpecialChars = "token-with:_.-special-chars";
        NotificationCreateTokenRequest request = new NotificationCreateTokenRequest(tokenWithSpecialChars);

        // when & then
        mockMvc.perform(post("/api/notifications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        // verify
        then(tokenRepository.findById(tokenWithSpecialChars)).isPresent();
    }
}
