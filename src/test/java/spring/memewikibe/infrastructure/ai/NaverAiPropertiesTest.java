package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NaverAiProperties 단위 테스트")
class NaverAiPropertiesTest {

    @Test
    @DisplayName("기본값이 올바르게 설정된다")
    void defaultValues_areSetCorrectly() {
        // given
        NaverAiProperties properties = new NaverAiProperties();

        // then
        assertThat(properties.getApiKey()).isEmpty();
        assertThat(properties.getRequestId()).isEqualTo("meme-wiki-qrewrite");
        assertThat(properties.getApiEndpoint())
            .isEqualTo("https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003");
    }

    @Test
    @DisplayName("프로퍼티 값을 설정할 수 있다")
    void properties_canBeSet() {
        // given
        NaverAiProperties properties = new NaverAiProperties();

        // when
        properties.setApiKey("custom-api-key");
        properties.setRequestId("custom-request-id");
        properties.setApiEndpoint("https://custom-endpoint.com");

        // then
        assertThat(properties.getApiKey()).isEqualTo("custom-api-key");
        assertThat(properties.getRequestId()).isEqualTo("custom-request-id");
        assertThat(properties.getApiEndpoint()).isEqualTo("https://custom-endpoint.com");
    }

    @Test
    @DisplayName("null 값으로 설정할 수 있다")
    void properties_canBeSetToNull() {
        // given
        NaverAiProperties properties = new NaverAiProperties();
        properties.setApiKey("test");
        properties.setRequestId("test");
        properties.setApiEndpoint("test");

        // when
        properties.setApiKey(null);
        properties.setRequestId(null);
        properties.setApiEndpoint(null);

        // then
        assertThat(properties.getApiKey()).isNull();
        assertThat(properties.getRequestId()).isNull();
        assertThat(properties.getApiEndpoint()).isNull();
    }
}
