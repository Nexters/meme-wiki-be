package spring.memewikibe.application.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.NotificationTokenRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemeNotificationService {

    private final MemeRepository memeRepository;
    private final NotificationTokenRepository tokenRepository;
    private final NotificationDispatchService dispatchService;

    public void sendMemeNotification(Long memeId, String title, String body) {
        Meme meme = memeRepository.findById(memeId)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));

        List<String> tokens = tokenRepository.findAllTokens();

        if (tokens.isEmpty()) {
            log.info("No active tokens found for meme notification: memeId={}", memeId);
            return;
        }

        log.info("Preparing to send meme notification: memeId={}, memeTitle: {}, memeImageUrl={}, notificationTitle={}, notificationBody={}, tokensCount={}",
            memeId, meme.getTitle(), meme.getImgUrl(), title, body, tokens.size());

        NotificationSender.NotificationSendCommand command = new NotificationSender.NotificationSendCommand(
            title,
            body,
            meme.getImgUrl(),
            Map.of(
                "meme_id", memeId.toString(),
                "deep_link", "/memes/" + memeId
            )
        );

        dispatchService.dispatch(command, tokens);
    }
}
