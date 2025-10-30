package spring.memewikibe.support.response;

import spring.memewikibe.support.error.ErrorMessage;
import spring.memewikibe.support.error.ErrorType;

/**
 * Standard API response wrapper for all REST endpoints.
 * Uses discriminated union pattern with resultType to indicate success or error.
 *
 * @param <S> the type of the success data
 */
public record ApiResponse<S>(
    ResultType resultType,
    S data,
    ErrorMessage error
) {
    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static ApiResponse<?> error(ErrorType error) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error));
    }

    public static ApiResponse<?> error(ErrorType error, Object errorData) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error, errorData));
    }
}
