package spring.memewikibe.external.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import spring.memewikibe.external.ClovaErrorCode;

@Getter
public class ClovaApiException extends RuntimeException {

    private final ClovaErrorCode clovaErrorCode;
    private final String clovaMessage;
    private final HttpStatus mappedHttpStatus;
    private final ErrorCategory category;
    private final boolean retryable;

    public ClovaApiException(ClovaErrorCode clovaErrorCode, String clovaMessage) {
        super(clovaMessage);
        this.clovaErrorCode = clovaErrorCode;
        this.clovaMessage = clovaMessage;
        this.mappedHttpStatus = clovaErrorCode.getHttpStatus();
        this.category = determineCategory(String.valueOf(clovaErrorCode.getCode()));
        this.retryable = clovaErrorCode.isRetryable();
    }

    public static ClovaApiException of(int errorCode, String message) {
        ClovaErrorCode clovaErrorCode = ClovaErrorCode.fromCode(errorCode);
        return new ClovaApiException(clovaErrorCode, message);
    }

    private static ErrorCategory determineCategory(String clovaCode) {
        return switch (clovaCode.substring(0, 3)) {
            case "401" -> ErrorCategory.AUTHENTICATION;
            case "403" -> ErrorCategory.AUTHORIZATION;
            case "400" -> ErrorCategory.CLIENT_ERROR;
            case "429" -> ErrorCategory.RATE_LIMIT;
            case "408" -> ErrorCategory.TIMEOUT;
            default -> ErrorCategory.SERVER_ERROR;
        };
    }

    public String getClovaCode() {
        return String.valueOf(clovaErrorCode.getCode());
    }


    public enum ErrorCategory {
        AUTHENTICATION,
        AUTHORIZATION,
        CLIENT_ERROR,
        RATE_LIMIT,
        TIMEOUT,
        SERVER_ERROR
    }
}