package spring.memewikibe.support.error;

/**
 * Error message structure for API error responses.
 * Contains error code, human-readable message, and optional additional data.
 */
public record ErrorMessage(
    String code,
    String message,
    Object data
) {
    public ErrorMessage(ErrorType errorType) {
        this(errorType.getCode().name(), errorType.getMessage(), null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.getCode().name(), errorType.getMessage(), data);
    }
}
