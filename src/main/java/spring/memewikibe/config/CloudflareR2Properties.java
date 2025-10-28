package spring.memewikibe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record CloudflareR2Properties(
    String accessKeyId,
    String secretAccessKey,
    String endpoint,
    String bucketName
) {
}
