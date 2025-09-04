package spring.memewikibe.external.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClovaAuthInterceptor implements RequestInterceptor {
    
    @Value("${clova.api-key}")
    private String clovaApiKey;
    
    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + clovaApiKey);
    }
}