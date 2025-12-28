package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.application.notification.NotificationSender.NotificationSendCommand;
import spring.memewikibe.application.notification.NotificationSender.SendResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("FcmNotificationSender 단위 테스트")
class FcmNotificationSenderTest {

    @Mock
    private FcmMessagingClient messagingClient;

    private FcmNotificationSender sut;

    @BeforeEach
    void setUp() {
        sut = new FcmNotificationSender(messagingClient);
    }

    @Test
    @DisplayName("send: null 토큰 리스트로 호출 시 빈 결과 반환")
    void send_returnsEmptyResult_whenTokensIsNull() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );

        // when
        SendResult result = sut.send(command, null);

        // then
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
        then(messagingClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("send: 빈 토큰 리스트로 호출 시 빈 결과 반환")
    void send_returnsEmptyResult_whenTokensIsEmpty() {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );

        // when
        SendResult result = sut.send(command, List.of());

        // then
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
        then(messagingClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("send: 알림을 성공적으로 전송")
    void send_succeeds_sendsNotificationSuccessfully() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("meme_id", "123", "deep_link", "/memes/123")
        );
        List<String> tokens = List.of("token1", "token2", "token3");

        BatchResponse batchResponse = createBatchResponse(3, 0, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();

        then(messagingClient).should().sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("send: imageUrl이 null인 경우 정상 처리")
    void send_succeeds_whenImageUrlIsNull() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            null,  // imageUrl is null
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1");

        BatchResponse batchResponse = createBatchResponse(1, 0, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();

        then(messagingClient).should().sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("send: data가 null인 경우 정상 처리")
    void send_succeeds_whenDataIsNull() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            null  // data is null
        );
        List<String> tokens = List.of("token1");

        BatchResponse batchResponse = createBatchResponse(1, 0, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();

        then(messagingClient).should().sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("send: data가 빈 맵인 경우 정상 처리")
    void send_succeeds_whenDataIsEmpty() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of()  // data is empty
        );
        List<String> tokens = List.of("token1");

        BatchResponse batchResponse = createBatchResponse(1, 0, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();

        then(messagingClient).should().sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("send: 일부 토큰 전송 실패 시 실패 수 반환")
    void send_succeeds_returnsFailureCountWhenSomeTokensFail() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "token2", "token3");

        BatchResponse batchResponse = createBatchResponse(2, 1, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    @DisplayName("send: INVALID_ARGUMENT 에러인 경우 무효한 토큰으로 표시")
    void send_succeeds_marksTokenAsInvalidWhenInvalidArgument() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "invalid-token", "token3");

        List<SendResponse> responses = List.of(
            createSuccessfulResponse(),
            createFailedResponse(MessagingErrorCode.INVALID_ARGUMENT),
            createSuccessfulResponse()
        );
        BatchResponse batchResponse = createBatchResponseWithResponses(2, 1, responses);
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly("invalid-token");
    }

    @Test
    @DisplayName("send: UNREGISTERED 에러인 경우 무효한 토큰으로 표시")
    void send_succeeds_marksTokenAsInvalidWhenUnregistered() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "unregistered-token", "token3");

        List<SendResponse> responses = List.of(
            createSuccessfulResponse(),
            createFailedResponse(MessagingErrorCode.UNREGISTERED),
            createSuccessfulResponse()
        );
        BatchResponse batchResponse = createBatchResponseWithResponses(2, 1, responses);
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly("unregistered-token");
    }

    @Test
    @DisplayName("send: SENDER_ID_MISMATCH 에러인 경우 무효한 토큰으로 표시")
    void send_succeeds_marksTokenAsInvalidWhenSenderIdMismatch() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "mismatched-token", "token3");

        List<SendResponse> responses = List.of(
            createSuccessfulResponse(),
            createFailedResponse(MessagingErrorCode.SENDER_ID_MISMATCH),
            createSuccessfulResponse()
        );
        BatchResponse batchResponse = createBatchResponseWithResponses(2, 1, responses);
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly("mismatched-token");
    }

    @Test
    @DisplayName("send: 다수의 무효한 토큰 처리")
    void send_succeeds_handlesMultipleInvalidTokens() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "invalid-token1", "token3", "invalid-token2");

        List<SendResponse> responses = List.of(
            createSuccessfulResponse(),
            createFailedResponse(MessagingErrorCode.INVALID_ARGUMENT),
            createSuccessfulResponse(),
            createFailedResponse(MessagingErrorCode.UNREGISTERED)
        );
        BatchResponse batchResponse = createBatchResponseWithResponses(2, 2, responses);
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(2);
        assertThat(result.invalidTokens()).containsExactly("invalid-token1", "invalid-token2");
    }

    @Test
    @DisplayName("send: FirebaseMessagingException 발생 시 실패 결과 반환")
    void send_returnsFailureResult_whenFirebaseMessagingExceptionOccurs() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1", "token2", "token3");

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        doThrow(exception).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(3);
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    @DisplayName("send: 단일 토큰으로도 정상 작동")
    void send_succeeds_withSingleToken() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of("token1");

        BatchResponse batchResponse = createBatchResponse(1, 0, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    @DisplayName("send: 다수의 토큰으로도 정상 작동")
    void send_succeeds_withManyTokens() throws FirebaseMessagingException {
        // given
        NotificationSendCommand command = new NotificationSendCommand(
            "제목",
            "내용",
            "https://example.com/image.jpg",
            Map.of("key", "value")
        );
        List<String> tokens = List.of(
            "token1", "token2", "token3", "token4", "token5",
            "token6", "token7", "token8", "token9", "token10"
        );

        BatchResponse batchResponse = createBatchResponse(10, 0, List.of());
        doReturn(batchResponse).when(messagingClient).sendEachForMulticast(any(MulticastMessage.class));

        // when
        SendResult result = sut.send(command, tokens);

        // then
        assertThat(result.successCount()).isEqualTo(10);
        assertThat(result.failureCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
    }

    private BatchResponse createBatchResponse(int successCount, int failureCount, List<SendResponse> customResponses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        doReturn(successCount).when(batchResponse).getSuccessCount();
        doReturn(failureCount).when(batchResponse).getFailureCount();

        if (customResponses.isEmpty()) {
            List<SendResponse> responses = new java.util.ArrayList<>();
            for (int i = 0; i < successCount; i++) {
                responses.add(createSuccessfulResponse());
            }
            doReturn(responses).when(batchResponse).getResponses();
        } else {
            doReturn(customResponses).when(batchResponse).getResponses();
        }

        return batchResponse;
    }

    private BatchResponse createBatchResponseWithResponses(int successCount, int failureCount, List<SendResponse> responses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        doReturn(successCount).when(batchResponse).getSuccessCount();
        doReturn(failureCount).when(batchResponse).getFailureCount();
        doReturn(responses).when(batchResponse).getResponses();
        return batchResponse;
    }

    private SendResponse createSuccessfulResponse() {
        SendResponse response = mock(SendResponse.class);
        doReturn(true).when(response).isSuccessful();
        return response;
    }

    private SendResponse createFailedResponse(MessagingErrorCode errorCode) {
        SendResponse response = mock(SendResponse.class);
        doReturn(false).when(response).isSuccessful();

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        doReturn(errorCode).when(exception).getMessagingErrorCode();
        doReturn("Error: " + errorCode).when(exception).getMessage();

        doReturn(exception).when(response).getException();
        return response;
    }
}
