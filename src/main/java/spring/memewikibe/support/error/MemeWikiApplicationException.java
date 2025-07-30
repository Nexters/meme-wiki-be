package spring.memewikibe.support.error;

import lombok.Getter;

@Getter
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

}
