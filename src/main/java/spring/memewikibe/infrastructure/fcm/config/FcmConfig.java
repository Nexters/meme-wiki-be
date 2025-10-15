package spring.memewikibe.infrastructure.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FcmConfig {

    private final FcmProperties fcmProperties;
    private final ResourceLoader resourceLoader;

    public FcmConfig(FcmProperties fcmProperties, ResourceLoader resourceLoader) {
        this.fcmProperties = fcmProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        String keyPath = fcmProperties.serviceAccountKeyPath();

        if (keyPath == null || keyPath.isBlank()) {
            log.warn("FCM service account key path is not configured. FCM notifications will not be available.");
            return;
        }

        log.info("Initializing FCM with service account key: {}", keyPath);

        try {
            Resource resource = resourceLoader.getResource(keyPath);
            if (!resource.exists()) {
                throw new IllegalStateException("FCM service account key not found: " + keyPath);
            }

            try (InputStream stream = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize FirebaseApp with service account key: " + keyPath, e);
        }
    }
}
