package spring.memewikibe.application.notification;

import org.springframework.stereotype.Component;
import spring.memewikibe.domain.notification.NotificationToken;
import spring.memewikibe.domain.notification.NotificationToken.DevicePlatform;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

@Component
public class NotificationTokenRegister {
    private final NotificationTokenRepository notificationTokenRepository;

    public NotificationTokenRegister(NotificationTokenRepository notificationTokenRepository) {
        this.notificationTokenRepository = notificationTokenRepository;
    }

    public void registerToken(String token, String deviceId, String platform) {
        notificationTokenRepository.save(NotificationToken.create(token, deviceId, DevicePlatform.valueOf(platform.toUpperCase())));
    }
}
