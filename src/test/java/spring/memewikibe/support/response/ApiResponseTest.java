package spring.memewikibe.support.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.support.error.ErrorMessage;
import spring.memewikibe.support.error.ErrorType;

import static org.assertj.core.api.BDDAssertions.then;

@UnitTest
class ApiResponseTest {

    @Nested
    @DisplayName("success() 메서드는")
    class SuccessMethod {

        @Test
        @DisplayName("데이터 없이 성공 응답을 생성한다")
        void createSuccessResponseWithoutData() {
            // when
            ApiResponse<?> response = ApiResponse.success();

            // then
            then(response.getResultType()).isEqualTo(ResultType.SUCCESS);
            then(response.getSuccess()).isNull();
            then(response.getError()).isNull();
        }

        @Test
        @DisplayName("데이터와 함께 성공 응답을 생성한다")
        void createSuccessResponseWithData() {
            // given
            String data = "test data";

            // when
            ApiResponse<String> response = ApiResponse.success(data);

            // then
            then(response.getResultType()).isEqualTo(ResultType.SUCCESS);
            then(response.getSuccess()).isEqualTo(data);
            then(response.getError()).isNull();
        }

        @Test
        @DisplayName("null 데이터로 성공 응답을 생성한다")
        void createSuccessResponseWithNullData() {
            // when
            ApiResponse<String> response = ApiResponse.success(null);

            // then
            then(response.getResultType()).isEqualTo(ResultType.SUCCESS);
            then(response.getSuccess()).isNull();
            then(response.getError()).isNull();
        }

        @Test
        @DisplayName("복잡한 객체로 성공 응답을 생성한다")
        void createSuccessResponseWithComplexObject() {
            // given
            TestData testData = new TestData("name", 123);

            // when
            ApiResponse<TestData> response = ApiResponse.success(testData);

            // then
            then(response.getResultType()).isEqualTo(ResultType.SUCCESS);
            then(response.getSuccess()).isEqualTo(testData);
            then(response.getError()).isNull();
        }
    }

    @Nested
    @DisplayName("error() 메서드는")
    class ErrorMethod {

        @Test
        @DisplayName("ErrorType만으로 에러 응답을 생성한다")
        void createErrorResponseWithErrorType() {
            // given
            ErrorType errorType = ErrorType.MEME_NOT_FOUND;

            // when
            ApiResponse<?> response = ApiResponse.error(errorType);

            // then
            then(response.getResultType()).isEqualTo(ResultType.ERROR);
            then(response.getSuccess()).isNull();
            then(response.getError()).isNotNull();
            then(response.getError().code()).isEqualTo(errorType.getCode().name());
            then(response.getError().message()).isEqualTo(errorType.getMessage());
            then(response.getError().data()).isNull();
        }

        @Test
        @DisplayName("ErrorType과 추가 데이터로 에러 응답을 생성한다")
        void createErrorResponseWithErrorTypeAndData() {
            // given
            ErrorType errorType = ErrorType.EXTERNAL_SERVICE_BAD_REQUEST;
            String errorData = "Invalid request format";

            // when
            ApiResponse<?> response = ApiResponse.error(errorType, errorData);

            // then
            then(response.getResultType()).isEqualTo(ResultType.ERROR);
            then(response.getSuccess()).isNull();
            then(response.getError()).isNotNull();
            then(response.getError().code()).isEqualTo(errorType.getCode().name());
            then(response.getError().message()).isEqualTo(errorType.getMessage());
            then(response.getError().data()).isEqualTo(errorData);
        }

        @Test
        @DisplayName("다양한 ErrorType으로 에러 응답을 생성한다")
        void createErrorResponseWithDifferentErrorTypes() {
            // when & then
            ApiResponse<?> notFoundResponse = ApiResponse.error(ErrorType.MEME_NOT_FOUND);
            then(notFoundResponse.getError().code()).isEqualTo("E404");

            ApiResponse<?> internalErrorResponse = ApiResponse.error(ErrorType.DEFAULT_ERROR);
            then(internalErrorResponse.getError().code()).isEqualTo("E500");

            ApiResponse<?> serviceErrorResponse = ApiResponse.error(ErrorType.EXTERNAL_SERVICE_ERROR);
            then(serviceErrorResponse.getError().code()).isEqualTo("E503");
        }

        @Test
        @DisplayName("복잡한 객체를 에러 데이터로 사용할 수 있다")
        void createErrorResponseWithComplexErrorData() {
            // given
            ErrorType errorType = ErrorType.EXTERNAL_SERVICE_BAD_REQUEST;
            TestData errorData = new TestData("field", 42);

            // when
            ApiResponse<?> response = ApiResponse.error(errorType, errorData);

            // then
            then(response.getResultType()).isEqualTo(ResultType.ERROR);
            then(response.getError()).isNotNull();
            then(response.getError().data()).isEqualTo(errorData);
        }

        @Test
        @DisplayName("null을 에러 데이터로 사용할 수 있다")
        void createErrorResponseWithNullErrorData() {
            // given
            ErrorType errorType = ErrorType.DEFAULT_ERROR;

            // when
            ApiResponse<?> response = ApiResponse.error(errorType, null);

            // then
            then(response.getResultType()).isEqualTo(ResultType.ERROR);
            then(response.getError()).isNotNull();
            then(response.getError().data()).isNull();
        }
    }

    @Nested
    @DisplayName("ApiResponse는")
    class ApiResponseSemantics {

        @Test
        @DisplayName("성공 응답은 에러 필드가 null이다")
        void successResponseHasNullError() {
            // when
            ApiResponse<?> emptySuccess = ApiResponse.success();
            ApiResponse<String> dataSuccess = ApiResponse.success("data");

            // then
            then(emptySuccess.getError()).isNull();
            then(dataSuccess.getError()).isNull();
        }

        @Test
        @DisplayName("에러 응답은 성공 필드가 null이다")
        void errorResponseHasNullSuccess() {
            // when
            ApiResponse<?> errorResponse1 = ApiResponse.error(ErrorType.MEME_NOT_FOUND);
            ApiResponse<?> errorResponse2 = ApiResponse.error(ErrorType.DEFAULT_ERROR, "data");

            // then
            then(errorResponse1.getSuccess()).isNull();
            then(errorResponse2.getSuccess()).isNull();
        }

        @Test
        @DisplayName("성공 응답의 resultType은 SUCCESS다")
        void successResponseHasSuccessResultType() {
            // when
            ApiResponse<?> response = ApiResponse.success("data");

            // then
            then(response.getResultType()).isEqualTo(ResultType.SUCCESS);
        }

        @Test
        @DisplayName("에러 응답의 resultType은 ERROR다")
        void errorResponseHasErrorResultType() {
            // when
            ApiResponse<?> response = ApiResponse.error(ErrorType.MEME_NOT_FOUND);

            // then
            then(response.getResultType()).isEqualTo(ResultType.ERROR);
        }
    }

    @Nested
    @DisplayName("ErrorMessage 통합 테스트")
    class ErrorMessageIntegration {

        @Test
        @DisplayName("ApiResponse의 에러는 올바른 ErrorMessage 구조를 가진다")
        void errorResponseHasCorrectErrorMessageStructure() {
            // given
            ErrorType errorType = ErrorType.CATEGORY_NOT_FOUND;
            String additionalData = "category-id-123";

            // when
            ApiResponse<?> response = ApiResponse.error(errorType, additionalData);

            // then
            ErrorMessage error = response.getError();
            then(error).isNotNull();
            then(error.code()).isEqualTo(errorType.getCode().name());
            then(error.message()).isEqualTo(errorType.getMessage());
            then(error.data()).isEqualTo(additionalData);
        }
    }

    // Test fixture
    record TestData(String name, int value) {}
}
