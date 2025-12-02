package spring.memewikibe.support.response;

import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.support.error.ErrorType;

import static org.assertj.core.api.BDDAssertions.then;

@UnitTest
class ApiResponseTest {

    @Test
    void success_데이터_없이_성공_응답을_생성한다() {
        // when
        ApiResponse<?> response = ApiResponse.success();

        // then
        then(response.getResultType()).isEqualTo(ResultType.SUCCESS);
        then(response.getSuccess()).isNull();
        then(response.getError()).isNull();
    }

    @Test
    void success_데이터와_함께_성공_응답을_생성한다() {
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
    void error_에러_타입으로_에러_응답을_생성한다() {
        // given
        ErrorType errorType = ErrorType.MEME_NOT_FOUND;

        // when
        ApiResponse<?> response = ApiResponse.error(errorType);

        // then
        then(response.getResultType()).isEqualTo(ResultType.ERROR);
        then(response.getSuccess()).isNull();
        then(response.getError()).isNotNull();
        then(response.getError().getCode()).isEqualTo(errorType.getCode().name());
        then(response.getError().getMessage()).isEqualTo(errorType.getMessage());
        then(response.getError().getData()).isNull();
    }

    @Test
    void error_에러_타입과_데이터로_에러_응답을_생성한다() {
        // given
        ErrorType errorType = ErrorType.MEME_NOT_FOUND;
        Object errorData = "additional error info";

        // when
        ApiResponse<?> response = ApiResponse.error(errorType, errorData);

        // then
        then(response.getResultType()).isEqualTo(ResultType.ERROR);
        then(response.getSuccess()).isNull();
        then(response.getError()).isNotNull();
        then(response.getError().getCode()).isEqualTo(errorType.getCode().name());
        then(response.getError().getMessage()).isEqualTo(errorType.getMessage());
        then(response.getError().getData()).isEqualTo(errorData);
    }

    @Test
    void success_다양한_타입의_데이터를_지원한다() {
        // given
        record TestData(String name, int value) {
        }
        TestData data = new TestData("test", 123);

        // when
        ApiResponse<TestData> response = ApiResponse.success(data);

        // then
        then(response.getSuccess()).isEqualTo(data);
        then(response.getSuccess().name()).isEqualTo("test");
        then(response.getSuccess().value()).isEqualTo(123);
    }
}
