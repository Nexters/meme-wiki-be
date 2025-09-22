package spring.memewikibe.application.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationTokenRegister notificationTokenRegister;

    @Override
    public void registerNotificationToken(String token, String deviceId, String platform) {
        notificationTokenRegister.registerToken(token, deviceId, platform);
    }
}
