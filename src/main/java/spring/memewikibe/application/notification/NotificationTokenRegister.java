package spring.memewikibe.application.notification;

import org.springframework.stereotype.Component;
import spring.memewikibe.domain.notification.NotificationToken;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

@Component
public class NotificationTokenRegister {
    private final NotificationTokenRepository notificationTokenRepository;

    public NotificationTokenRegister(NotificationTokenRepository notificationTokenRepository) {
        this.notificationTokenRepository = notificationTokenRepository;
    }

    public void registerToken(String token) {
        notificationTokenRepository.save(NotificationToken.create(token));
    }
}
