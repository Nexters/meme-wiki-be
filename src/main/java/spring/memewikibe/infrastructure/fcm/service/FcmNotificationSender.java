package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.memewikibe.application.notification.NotificationSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSender {

    private final FcmMessagingClient messagingClient;

    @Override
    public SendResult send(NotificationSendCommand command, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return new SendResult(0, 0, List.of());
        }

        Notification.Builder notificationBuilder = Notification.builder()
            .setTitle(command.title())
            .setBody(command.body());

        if (command.imageUrl() != null && !command.imageUrl().isBlank()) {
            notificationBuilder.setImage(command.imageUrl());
        }

        Notification notification = notificationBuilder.build();

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
            .setNotification(notification)
            .addAllTokens(tokens);

        Map<String, String> data = command.data();
        if (data != null && !data.isEmpty()) {
            data.forEach(messageBuilder::putData);
        }

        MulticastMessage message = messageBuilder.build();

        try {
            BatchResponse response = messagingClient.sendEachForMulticast(message);
            return toSendResult(response, tokens);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to {} tokens", tokens.size(), e);
            return new SendResult(0, tokens.size(), List.of());
        }
    }

    private SendResult toSendResult(BatchResponse response, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();

        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode code = sendResponse.getException().getMessagingErrorCode();
                if (code == MessagingErrorCode.INVALID_ARGUMENT ||
                    code == MessagingErrorCode.UNREGISTERED ||
                    code == MessagingErrorCode.SENDER_ID_MISMATCH) {
                    invalidTokens.add(tokens.get(i));
                }
            }
        }

        return new SendResult(response.getSuccessCount(), response.getFailureCount(), invalidTokens);
    }
}
