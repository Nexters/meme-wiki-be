package spring.memewikibe.external.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class ClovaApiException extends RuntimeException {

    private final String clovaCode;
    private final String clovaMessage;
    private final HttpStatus mappedHttpStatus;
    private final ErrorCategory category;
    private final boolean retryable;

    public ClovaApiException(String clovaCode, String clovaMessage, HttpStatusCode originalStatus) {
        super(clovaMessage);
        this.clovaCode = clovaCode;
        this.clovaMessage = clovaMessage;
        this.mappedHttpStatus = mapToHttpStatus(clovaCode, originalStatus);
        this.category = determineCategory(clovaCode);
        this.retryable = isRetryableError(clovaCode);
    }


    private static HttpStatus mapToHttpStatus(String clovaCode, HttpStatusCode originalStatus) {
        return switch (clovaCode) {
            case "40100", "40101", "40102", "40103", "40104" -> HttpStatus.UNAUTHORIZED;
            case "40300", "40301", "40170" -> HttpStatus.FORBIDDEN;
            case "40000", "40001", "40002", "40003", "40004", "40005", "40009", "40055", "40060", "40061", "40063" ->
                HttpStatus.BAD_REQUEST;
            case "40400" -> HttpStatus.NOT_FOUND;
            case "42900", "42901", "42902", "42903" -> HttpStatus.TOO_MANY_REQUESTS;
            case "40800" -> HttpStatus.REQUEST_TIMEOUT;
            case "41300" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "41500", "41501" -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case "42400" -> HttpStatus.FAILED_DEPENDENCY;
            default -> HttpStatus.valueOf(originalStatus.value());
        };
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

    private static boolean isRetryableError(String clovaCode) {
        return switch (clovaCode) {
            case "40800", "42900", "42901", "42902", "42903" -> true; // 타임아웃, 레이트 리미트
            case "40102", "40103" -> true; // 토큰 만료 - 재발급 후 재시도 가능
            default -> clovaCode.startsWith("5"); // 5xx 서버 에러
        };
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