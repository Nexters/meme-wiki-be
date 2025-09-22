package spring.memewikibe.application.notification;

public interface NotificationService {
    void registerNotificationToken(String token, String deviceId, String platform);
}
