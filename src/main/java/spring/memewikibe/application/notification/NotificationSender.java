package spring.memewikibe.application.notification;

import java.util.List;
import java.util.Map;

public interface NotificationSender {

    SendResult send(NotificationSendCommand command, List<String> tokens);

    record NotificationSendCommand(
        String title,
        String body,
        String imageUrl,
        Map<String, String> data
    ) {
    }

    record SendResult(
        int successCount,
        int failureCount,
        List<String> invalidTokens
    ) {
    }
}
