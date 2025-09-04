package spring.memewikibe.external.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.BDDAssertions.then;

class ClovaAuthInterceptorTest {

    @Test
    void apply_injectsAuthorizationHeaderWithBearerToken() throws Exception {
        // given
        ClovaAuthInterceptor interceptor = new ClovaAuthInterceptor();
        setField(interceptor, "clovaApiKey", "test-key");
        RequestTemplate template = new RequestTemplate();

        // when
        interceptor.apply(template);

        // then
        then(template.headers())
            .containsKey("Authorization");
        then(template.headers().get("Authorization"))
            .singleElement()
            .isEqualTo("Bearer test-key");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

