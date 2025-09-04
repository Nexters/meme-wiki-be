package spring.memewikibe.external;

import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

public class ClovaException extends MemeWikiApplicationException {
    public ClovaException(ErrorType errorType) {
        super(errorType);
    }
    public ClovaException(ErrorType errorType, Object data) {
        super(errorType, data);
    }

}