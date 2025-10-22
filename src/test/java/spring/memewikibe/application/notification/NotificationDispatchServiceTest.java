package spring.memewikibe.application.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.application.notification.NotificationSender.NotificationSendCommand;
import spring.memewikibe.application.notification.NotificationSender.SendResult;
import spring.memewikibe.infrastructure.NotificationTokenRepository;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @InjectMocks
    private NotificationDispatchService sut;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationTokenRepository tokenRepository;

    @Test
    @DisplayName("알림을 성공적으로 전송한다")
    void dispatch_성공() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "token2", "token3");

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(3, 0, List.of()));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 전송 중 무효한 토큰이 있으면 해당 토큰을 삭제한다")
    void dispatch_무효한_토큰_삭제() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            null,
            Map.of()
        );
        List<String> tokens = List.of("token1", "token2", "token3");
        List<String> invalidTokens = List.of("token2");

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(2, 1, invalidTokens));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).should().deleteById("token2");
    }

    @Test
    @DisplayName("여러 무효한 토큰이 있으면 모두 삭제한다")
    void dispatch_여러_무효한_토큰_삭제() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("meme_id", "123")
        );
        List<String> tokens = List.of("token1", "token2", "token3", "token4");
        List<String> invalidTokens = List.of("token1", "token3", "token4");

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(1, 3, invalidTokens));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).should().deleteById("token1");
        then(tokenRepository).should().deleteById("token3");
        then(tokenRepository).should().deleteById("token4");
        then(tokenRepository).should(times(3)).deleteById(anyString());
    }

    @Test
    @DisplayName("토큰 삭제 중 예외가 발생해도 다른 토큰 삭제는 계속 진행한다")
    void dispatch_토큰_삭제_예외_처리() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            null,
            null
        );
        List<String> tokens = List.of("token1", "token2", "token3");
        List<String> invalidTokens = List.of("token1", "token2", "token3");

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(0, 3, invalidTokens));

        // 첫 번째 토큰 삭제 시 예외 발생
        doThrow(new RuntimeException("DB error"))
            .when(tokenRepository).deleteById("token1");

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).should().deleteById("token1");
        then(tokenRepository).should().deleteById("token2");
        then(tokenRepository).should().deleteById("token3");
    }

    @Test
    @DisplayName("알림 전송이 실패해도 예외를 던지지 않는다")
    void dispatch_전송_실패_처리() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of()
        );
        List<String> tokens = List.of("token1", "token2");

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(0, 2, List.of()));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 토큰 리스트로 호출해도 정상 처리된다")
    void dispatch_빈_토큰_리스트() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            null,
            Map.of()
        );
        List<String> tokens = List.of();

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(0, 0, List.of()));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("null이 아닌 모든 필드를 가진 command로 전송한다")
    void dispatch_전체_필드_포함() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "새로운 밈이 등록되었습니다",
            "지금 바로 확인해보세요!",
            "https://cdn.example.com/meme-image.jpg",
            Map.of(
                "meme_id", "42",
                "deep_link", "/memes/42",
                "category", "humor"
            )
        );
        List<String> tokens = List.of("fcm-token-abc123", "fcm-token-def456");

        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(2, 0, List.of()));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일부 성공, 일부 실패, 일부 무효한 경우를 올바르게 처리한다")
    void dispatch_혼합_결과_처리() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("test", "data")
        );
        List<String> tokens = List.of("valid1", "valid2", "invalid1", "failed1", "invalid2");
        List<String> invalidTokens = List.of("invalid1", "invalid2");

        // 2 성공, 2 실패 (재시도 가능), 2 무효 (토큰 삭제 필요)
        given(notificationSender.send(command, tokens))
            .willReturn(new SendResult(2, 3, invalidTokens));

        // when
        sut.dispatch(command, tokens);

        // then
        then(notificationSender).should().send(command, tokens);
        then(tokenRepository).should().deleteById("invalid1");
        then(tokenRepository).should().deleteById("invalid2");
        then(tokenRepository).should(times(2)).deleteById(anyString());
    }
}
