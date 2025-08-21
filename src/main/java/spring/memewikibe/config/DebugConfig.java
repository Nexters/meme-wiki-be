package spring.memewikibe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 환경변수 로딩 디버깅용 설정
 */
@Slf4j
@Configuration
public class DebugConfig implements CommandLineRunner {

    @Value("${VERTEX_AI_PROJECT_ID:NOT_SET}")
    private String vertexProjectId;

    @Value("${PINECONE_API_KEY:NOT_SET}")
    private String pineconeApiKey;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Environment Variables Debug ===");
        log.info("VERTEX_AI_PROJECT_ID: {}", vertexProjectId);
        log.info("PINECONE_API_KEY: {}", pineconeApiKey.length() > 10 ? pineconeApiKey.substring(0, 10) + "..." : "NOT_SET");
        log.info("=== End Debug ===");
    }
}
