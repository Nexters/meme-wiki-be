package spring.memewikibe.support.error;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorMessage {
    private final String code;
    private final String message;
    private final Object data;

    public ErrorMessage(ErrorType errorType) {
        this(errorType.getCode().name(), errorType.getMessage(), null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.getCode().name(), errorType.getMessage(), data);
    }
}
