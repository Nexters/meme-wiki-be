package spring.memewikibe.external;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ClovaErrorCode {

    BAD_REQUEST(40000, "Bad request", HttpStatus.BAD_REQUEST, false),
    INVALID_PARAMETER(40001, "Invalid parameter", HttpStatus.BAD_REQUEST, false),
    UNSUPPORTED_PARAMETER(40002, "Unsupported parameter", HttpStatus.BAD_REQUEST, false),
    CONTEXT_LENGTH_EXCEEDED(40003, "Context length exceeded", HttpStatus.BAD_REQUEST, false),
    TEXT_EMPTY(40004, "Text empty", HttpStatus.BAD_REQUEST, false),
    UNAVAILABLE_OUTPUT(40005, "Unavailable output", HttpStatus.BAD_REQUEST, false),
    UNSUPPORTED_FUNCTION(40009, "Unsupported function", HttpStatus.BAD_REQUEST, false),
    INVALID_RESPONSE_FORMAT_SCHEMA(40055, "Invalid response format schema", HttpStatus.BAD_REQUEST, false),
    UNSUPPORTED_IMAGE_FORMAT(40060, "Unsupported image format", HttpStatus.BAD_REQUEST, false),
    FILE_SIZE_ERROR(40061, "File size error", HttpStatus.PAYLOAD_TOO_LARGE, false),
    INVALID_IMAGE(40063, "Invalid image", HttpStatus.BAD_REQUEST, true),
    MODEL_NOT_FOUND(40080, "Model not found", HttpStatus.NOT_FOUND, false),
    MODEL_FADE_OUT(40082, "Model fade out", HttpStatus.GONE, false),
    MODEL_NO_RESOURCE(40083, "Model no resource", HttpStatus.SERVICE_UNAVAILABLE, false),
    UNSUPPORTED_API_FOR_MODEL(40084, "Unsupported API for model", HttpStatus.BAD_REQUEST, false),

    UNAUTHORIZED(40100, "Unauthorized", HttpStatus.UNAUTHORIZED, false),
    INVALID_SIGNATURE(40101, "Invalid signature", HttpStatus.UNAUTHORIZED, false),
    INVALID_ACCESS_TOKEN(40102, "Invalid access token", HttpStatus.UNAUTHORIZED, false),
    ACCESS_TOKEN_EXPIRED(40103, "Access token expired", HttpStatus.UNAUTHORIZED, false),
    INVALID_KEY(40104, "Invalid key", HttpStatus.UNAUTHORIZED, false),
    NO_SUBSCRIPTION_HISTORY(40170, "No subscription request history found. Please request a subscription to use", HttpStatus.FORBIDDEN, false),

    FORBIDDEN(40300, "Forbidden", HttpStatus.FORBIDDEN, false),
    NO_OWNERSHIP(40301, "No ownership", HttpStatus.FORBIDDEN, false),

    NOT_FOUND(40400, "Not found", HttpStatus.NOT_FOUND, false),

    NOT_ACCEPTABLE(40600, "Not Acceptable", HttpStatus.NOT_ACCEPTABLE, false),

    REQUEST_TIMEOUT(40800, "Request timeout", HttpStatus.REQUEST_TIMEOUT, true),

    REQUEST_BODY_SIZE_EXCEEDED(41300, "Request body size exceeded", HttpStatus.PAYLOAD_TOO_LARGE, false),

    MEDIATYPE_ERROR(41500, "MediaType error", HttpStatus.UNSUPPORTED_MEDIA_TYPE, false),
    NO_MULTIPART_BOUNDARY(41501, "No multipart boundary Content-Type", HttpStatus.UNSUPPORTED_MEDIA_TYPE, false),

    PROCESSING_FAILED(42400, "Processing Failed", HttpStatus.FAILED_DEPENDENCY, true),

    TOO_MANY_REQUESTS(42900, "Too many requests", HttpStatus.TOO_MANY_REQUESTS, true),
    RATE_EXCEEDED(42901, "Too many requests - rate exceeded", HttpStatus.TOO_MANY_REQUESTS, true),
    OVERLOADED(42902, "Too many requests - overloaded", HttpStatus.TOO_MANY_REQUESTS, true),
    IMAGE_QUEUE_FULL(42903, "Too many requests - image queue", HttpStatus.TOO_MANY_REQUESTS, true),

    INTERNAL_SERVER_ERROR(50000, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR, true),
    NOT_YET_IMPLEMENTED(50100, "Not yet implemented", HttpStatus.NOT_IMPLEMENTED, false),
    GATEWAY_TIMEOUT(50400, "Gateway timeout", HttpStatus.GATEWAY_TIMEOUT, true),

    UNKNOWN_ERROR(-1, "Unknown error", HttpStatus.INTERNAL_SERVER_ERROR, false);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean retryable;

    public static ClovaErrorCode fromCode(int code) {
        return Arrays.stream(values())
            .filter(errorCode -> errorCode.code == code)
            .findFirst()
            .orElse(UNKNOWN_ERROR);
    }
}