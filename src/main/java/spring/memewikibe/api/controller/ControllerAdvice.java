package spring.memewikibe.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import spring.memewikibe.external.exception.ClovaApiException;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;
import spring.memewikibe.support.response.ApiResponse;

import java.util.Map;

@RestControllerAdvice
public class ControllerAdvice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(MemeWikiApplicationException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(MemeWikiApplicationException e) {
        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("CustomException : {}", e.getMessage(), e);
            case WARN -> log.warn("CustomException : {}", e.getMessage(), e);
            default -> log.info("CustomException : {}", e.getMessage(), e);
        }
        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getData()), e.getErrorType().getStatus());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        if (e.getMessage().contains("favicon.ico")) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.warn("Static resource not found: {}", e.getMessage());
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(ClovaApiException.class)
    public ResponseEntity<ApiResponse<?>> handleClovaApiException(ClovaApiException e) {
        // 로그에는 상세한 Clova 에러 정보 기록
        log.error("Clova API Error - Code: {}, Category: {}, Retryable: {}, Message: {}",
            e.getClovaCode(), e.getCategory(), e.isRetryable(), e.getMessage(), e);

        ErrorType errorType = switch (e.getCategory()) {
            case AUTHENTICATION -> ErrorType.EXTERNAL_SERVICE_UNAUTHORIZED;
            case RATE_LIMIT -> ErrorType.EXTERNAL_SERVICE_TOO_MANY_REQUESTS;
            case CLIENT_ERROR -> ErrorType.EXTERNAL_SERVICE_BAD_REQUEST;
            default -> ErrorType.EXTERNAL_SERVICE_ERROR;
        };

        var errorData = Map.of(
            "clovaCode", e.getClovaCode(),
            "clovaOriginalMessage", e.getClovaMessage(),
            "category", e.getCategory().name(),
            "retryable", e.isRetryable()
        );

        return new ResponseEntity<>(
            ApiResponse.error(errorType, errorData),
            errorType.getStatus()
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Exception : {}", e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error(ErrorType.DEFAULT_ERROR), ErrorType.DEFAULT_ERROR.getStatus());
    }

}
