package spring.memewikibe.application.notification;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.notification.NotificationToken;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

@Component
public class NotificationTokenRegister {
    private final NotificationTokenRepository notificationTokenRepository;

    public NotificationTokenRegister(NotificationTokenRepository notificationTokenRepository) {
        this.notificationTokenRepository = notificationTokenRepository;
    }

    @Transactional
    public void registerToken(@NonNull final String token) {
        if (!notificationTokenRepository.existsById(token)) {
            notificationTokenRepository.save(NotificationToken.create(token));
        }
    }
}
