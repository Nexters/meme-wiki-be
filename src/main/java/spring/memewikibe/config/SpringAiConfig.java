package spring.memewikibe.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 관련 Bean 설정
 */
@Configuration
public class SpringAiConfig {

    /**
     * ChatClient Bean 설정
     * Vertex AI Gemini 모델을 사용하여 대화형 AI 클라이언트를 생성합니다.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 한국의 밈 문화에 정통한 전문가입니다.
                        사용자의 상황에 맞는 밈을 추천하고, 그 이유를 친근하고 재치있게 설명해주세요.
                        한국어로 자연스럽고 공감할 수 있는 방식으로 응답해주세요.
                        """)
                .build();
    }
}
