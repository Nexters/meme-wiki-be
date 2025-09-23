package spring.memewikibe.application.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationSender notificationSender;
    private final NotificationTokenRepository tokenRepository;

    @Async("fcmExecutor")
    public void dispatch(NotificationSender.NotificationSendCommand command, List<String> tokens) {
        NotificationSender.SendResult result = notificationSender.send(command, tokens);

        if (!result.invalidTokens().isEmpty()) {
            result.invalidTokens().forEach(token -> {
                try {
                    tokenRepository.deleteById(token);
                } catch (Exception e) {
                    log.error("Failed to remove invalid token: {}", token, e);
                }
            });
        }

        log.info("Notification dispatched - success={}, failure={}, invalidRemoved={}",
            result.successCount(), result.failureCount(), result.invalidTokens().size());
    }
}
