package spring.memewikibe.domain.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class NotificationToken {

    @Id
    private String token;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private DevicePlatform platform;

    private boolean isActive;

    private boolean pushEnabled;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @Builder
    private NotificationToken(String token, String deviceId, DevicePlatform platform, boolean isActive, boolean pushEnabled) {
        this.token = token;
        this.deviceId = deviceId;
        this.platform = platform;
        this.isActive = isActive;
        this.pushEnabled = pushEnabled;
    }

    public static NotificationToken create(String token, String deviceId, DevicePlatform platform) {
        return NotificationToken.builder()
            .token(token)
            .deviceId(deviceId)
            .platform(platform)
            .isActive(true)
            .pushEnabled(true)
            .build();
    }

    public enum DevicePlatform {
        ANDROID,
        IOS;
    }
}
