package spring.memewikibe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.Optional;

@Slf4j
@Configuration
public class CloudflareR2Config {

    private final CloudflareR2Properties properties;

    public CloudflareR2Config(CloudflareR2Properties properties) {
        this.properties = properties;
    }

    @Bean("s3Client")
    @ConditionalOnProperty(prefix = "cloudflare.r2", name = {"endpoint", "access-key-id", "secret-access-key", "bucket-name"})
    public S3Client r2Client() {
        logConfiguration();

        String endpoint = properties.endpoint();
        String accessKeyId = properties.accessKeyId();
        String secretAccessKey = properties.secretAccessKey();
        String bucketName = properties.bucketName();

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Cloudflare R2 endpoint is missing");
        }
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalStateException("Cloudflare R2 accessKeyId is missing");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new IllegalStateException("Cloudflare R2 secretAccessKey is missing");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("Cloudflare R2 bucketName is missing");
        }

        return createR2Client(endpoint, accessKeyId, secretAccessKey);
    }
    
    private void logConfiguration() {
        log.info("Cloudflare R2 설정 로드:");
        log.info("Access Key ID: {}", Optional.ofNullable(properties.accessKeyId()).map(this::maskSensitiveData).orElse("미설정"));
        log.info("Secret Access Key: {}", Optional.ofNullable(properties.secretAccessKey()).map(key -> "***설정됨***").orElse("미설정"));
        log.info("Endpoint: {}", Optional.ofNullable(properties.endpoint()).orElse("미설정"));
        log.info("Bucket Name: {}", Optional.ofNullable(properties.bucketName()).orElse("미설정"));
    }
    
    private S3Client createR2Client(String endpoint, String accessKeyId, String secretAccessKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @Qualifier("r2BucketName")
    @ConditionalOnProperty(prefix = "cloudflare.r2", name = {"bucket-name"})
    public String bucketName() {
        return properties.bucketName();
    }

    private String maskSensitiveData(String sensitiveData) {
        return Optional.ofNullable(sensitiveData)
                .filter(data -> data.length() > 8)
                .map(data -> data.substring(0, 4) + "***" + data.substring(data.length() - 4))
                .orElse("***");
    }
} 