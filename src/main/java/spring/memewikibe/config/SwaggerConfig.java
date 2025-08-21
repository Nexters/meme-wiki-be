package spring.memewikibe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MemeWikiBE API")
                .version("v0.0.1")
                .description("MemeWikiBE API 명세서"))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Production Server (HTTPS)")
            ));
    }
}