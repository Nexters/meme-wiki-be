package spring.memewikibe.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LocalDateTimeProvider implements TimeProvider {
    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
