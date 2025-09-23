package spring.memewikibe.infrastructure.fcm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fcm")
public record FcmProperties(
    String serviceAccountKeyPath
) {
}
