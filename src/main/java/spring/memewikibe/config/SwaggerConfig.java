package spring.memewikibe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(buildApiInfo())
            .servers(buildServers());
    }

    private Info buildApiInfo() {
        return new Info()
            .title("MemeWikiBE API")
            .version("v0.0.1")
            .description("""
                MemeWikiBE API 명세서

                이 API는 밈(Meme) 정보를 제공하고, 사용자가 밈을 검색, 공유, 커스터마이징할 수 있는 기능을 제공합니다.

                주요 기능:
                - 밈 검색 및 조회
                - 카테고리별 밈 탐색
                - 밈 공유 및 커스터마이징 통계
                - 밈 추천 및 랭킹
                - 퀴즈 기능
                """);
    }

    private List<Server> buildServers() {
        List<Server> servers = new ArrayList<>();

        // Add environment-specific server based on active profile
        switch (activeProfile) {
            case "prod" -> servers.add(new Server()
                .url("https://api.meme-wiki.net")
                .description("Production Server"));
            case "dev" -> {
                servers.add(new Server()
                    .url("https://api.meme-wiki.net")
                    .description("Development Server"));
                servers.add(new Server()
                    .url("http://localhost:8080")
                    .description("Local Development"));
            }
            default -> { // local profile
                servers.add(new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"));
            }
        }

        return servers;
    }
}