package spring.memewikibe.external.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.memewikibe.external.interceptor.ClovaAuthInterceptor;

@Configuration
public class ClovaFeignConfig {
    
    @Bean
    public RequestInterceptor clovaAuthInterceptor() {
        return new ClovaAuthInterceptor();
    }
}