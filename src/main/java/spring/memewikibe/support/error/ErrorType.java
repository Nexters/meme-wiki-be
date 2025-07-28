package spring.memewikibe.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ErrorType {
    DEFAULT_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "예기치 못한 에러가 발생했습니다.", LogLevel.ERROR),
    MEME_NOT_FOUNT(
        HttpStatus.NOT_FOUND, ErrorCode.E404, "존재하지 않는 밈입니다.", LogLevel.WARN),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404, "존재하지 않는 카테고리입니다.", LogLevel.WARN);

    private final HttpStatus status;

    private final ErrorCode code;

    private final String message;

    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {

        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }
}
