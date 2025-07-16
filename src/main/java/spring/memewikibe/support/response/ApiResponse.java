package spring.memewikibe.support.response;

import spring.memewikibe.support.error.ErrorMessage;
import spring.memewikibe.support.error.ErrorType;

public class ApiResponse<S> {
    private final ResultType resultType;
    private final S success;
    private final ErrorMessage error;

    public ApiResponse(ResultType resultType, S success, ErrorMessage error) {
        this.resultType = resultType;
        this.success = success;
        this.error = error;
    }

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

    public ResultType getResultType() {
        return resultType;
    }

    public S getSuccess() {
        return success;
    }

    public ErrorMessage getError() {
        return error;
    }
}
