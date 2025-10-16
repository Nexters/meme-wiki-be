package spring.memewikibe.application.notification;

import org.springframework.lang.NonNull;

public interface NotificationService {
    void registerNotificationToken(@NonNull final String token);
}
