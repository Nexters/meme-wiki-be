package spring.memewikibe.support.error;

public class MemeWikiApplicationException extends RuntimeException {
    private final ErrorType errorType;
    private final Object data;

    public MemeWikiApplicationException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
    }

    public MemeWikiApplicationException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Object getData() {
        return data;
    }

}
