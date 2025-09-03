package spring.memewikibe.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "spring.memewikibe.external")
public class FeignConfig {
}
