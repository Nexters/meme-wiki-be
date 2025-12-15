package spring.memewikibe.support.error;

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
