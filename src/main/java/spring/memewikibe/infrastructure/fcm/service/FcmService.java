package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.notification.NotificationToken;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.NotificationTokenRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class FcmService {

    private final NotificationTokenRepository notificationTokenRepository;
    private final MemeRepository memeRepository;

    @Async("fcmExecutor")
    public void sendMemeNotification(Long memeId, String title, String body) {
        Meme meme = memeRepository.findById(memeId)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));

        List<String> activeTokens = getActiveTokens();

        if (activeTokens.isEmpty()) {
            log.info("No active tokens found for meme notification: memeId={}", memeId);
            return;
        }

        Notification.Builder notificationBuilder = Notification.builder()
            .setTitle(title)
            .setBody(body);

        if (meme.getImgUrl() != null && !meme.getImgUrl().isBlank()) {
            notificationBuilder.setImage(meme.getImgUrl());
        }

        Notification notification = notificationBuilder.build();

        MulticastMessage multicastMessage = MulticastMessage.builder()
            .setNotification(notification)
            .addAllTokens(activeTokens)
            .putData("meme_id", memeId.toString())
            .putData("deep_link", "/memes/" + memeId)
            .build();

        sendMulticastMessage(multicastMessage, activeTokens);

        log.info("Meme notification sent: memeId={}, title={}, recipients={}", memeId, title, activeTokens.size());
    }

    private List<String> getActiveTokens() {
        return notificationTokenRepository.findAll()
            .stream()
            .map(NotificationToken::getToken)
            .toList();
    }

    private void sendMulticastMessage(MulticastMessage message, List<String> tokens) {
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            handleBatchResponse(response, tokens);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to {} tokens", tokens.size(), e);
        }
    }

    private void handleBatchResponse(BatchResponse response, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < response.getResponses().size(); i++) {
            SendResponse sendResponse = response.getResponses().get(i);

            if (!sendResponse.isSuccessful()) {
                String token = tokens.get(i);
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();

                log.warn("Failed to send message to token: {}, error: {}",
                    token.substring(0, Math.min(10, token.length())) + "...", errorCode);

                if (isTokenInvalid(errorCode)) {
                    invalidTokens.add(token);
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            removeInvalidTokens(invalidTokens);
        }

        log.info("FCM batch result - Success: {}, Failed: {}, Invalid tokens removed: {}",
            response.getSuccessCount(), response.getFailureCount(), invalidTokens.size());
    }

    private boolean isTokenInvalid(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
            errorCode == MessagingErrorCode.UNREGISTERED ||
            errorCode == MessagingErrorCode.SENDER_ID_MISMATCH;
    }

    private void removeInvalidTokens(List<String> invalidTokens) {
        for (String token : invalidTokens) {
            try {
                notificationTokenRepository.deleteById(token);
                log.debug("Removed invalid token: {}...",
                    token.substring(0, Math.min(10, token.length())));
            } catch (Exception e) {
                log.error("Failed to remove invalid token: {}...",
                    token.substring(0, Math.min(10, token.length())), e);
            }
        }
    }
}
