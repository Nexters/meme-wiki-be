package spring.memewikibe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
            .info(new Info()
                .title("MemeWikiBE API")
                .version("v0.0.1")
                .description("MemeWikiBE API 명세서"));

        if ("prod".equals(activeProfile)) {
            openAPI.servers(List.of(
                new Server()
                    .url("https://meme-wiki.net")
                    .description("Production Server (HTTPS)")
            ));
        } else {
            openAPI.servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server (HTTP)"),
                new Server()
                    .url("https://localhost:8080")
                    .description("Local Development Server (HTTPS)")
            ));
        }

        return openAPI;
    }

}
