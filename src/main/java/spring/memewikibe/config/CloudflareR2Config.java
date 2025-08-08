package spring.memewikibe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.Optional;

@Slf4j
@Configuration
public class CloudflareR2Config {

    @Value("${cloudflare.r2.access-key-id:}")
    private String accessKeyId;
    
    @Value("${cloudflare.r2.secret-access-key:}")
    private String secretAccessKey;
    
    @Value("${cloudflare.r2.endpoint:}")
    private String endpoint;
    
    @Value("${cloudflare.r2.bucket-name:}")
    private String bucketName;

    @Bean("s3Client")
    public S3Client r2Client() {
        logConfiguration();

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

        return createR2Client(endpoint);
    }
    
    private void logConfiguration() {
        log.info("Cloudflare R2 설정 로드:");
        log.info("Access Key ID: {}", Optional.ofNullable(accessKeyId).map(this::maskSensitiveData).orElse("미설정"));
        log.info("Secret Access Key: {}", Optional.ofNullable(secretAccessKey).map(key -> "***설정됨***").orElse("미설정"));
        log.info("Endpoint: {}", Optional.ofNullable(endpoint).orElse("미설정"));
        log.info("Bucket Name: {}", Optional.ofNullable(bucketName).orElse("미설정"));
    }
    
    private S3Client createR2Client(String endpoint) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public String bucketName() {
        return bucketName;
    }

    private String maskSensitiveData(String sensitiveData) {
        return Optional.ofNullable(sensitiveData)
                .filter(data -> data.length() > 8)
                .map(data -> data.substring(0, 4) + "***" + data.substring(data.length() - 4))
                .orElse("***");
    }
} 