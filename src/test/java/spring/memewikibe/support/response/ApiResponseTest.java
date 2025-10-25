package spring.memewikibe.support.response;

import org.junit.jupiter.api.Test;
import spring.memewikibe.support.error.ErrorType;

import static org.assertj.core.api.BDDAssertions.then;

class ApiResponseTest {

    @Test
    void success_메서드는_데이터_없이_성공_응답을_생성한다() {
        // when
        ApiResponse<?> response = ApiResponse.success();

        // then
        then(response.resultType()).isEqualTo(ResultType.SUCCESS);
        then(response.data()).isNull();
        then(response.error()).isNull();
    }

    @Test
    void success_메서드는_데이터와_함께_성공_응답을_생성한다() {
        // given
        String data = "test data";

        // when
        ApiResponse<String> response = ApiResponse.success(data);

        // then
        then(response.resultType()).isEqualTo(ResultType.SUCCESS);
        then(response.data()).isEqualTo(data);
        then(response.error()).isNull();
    }

    @Test
    void error_메서드는_에러_타입만으로_에러_응답을_생성한다() {
        // given
        ErrorType errorType = ErrorType.MEME_NOT_FOUND;

        // when
        ApiResponse<?> response = ApiResponse.error(errorType);

        // then
        then(response.resultType()).isEqualTo(ResultType.ERROR);
        then(response.data()).isNull();
        then(response.error()).isNotNull();
        then(response.error().code()).isEqualTo(errorType.getCode().name());
        then(response.error().message()).isEqualTo(errorType.getMessage());
        then(response.error().data()).isNull();
    }

    @Test
    void error_메서드는_에러_타입과_추가_데이터로_에러_응답을_생성한다() {
        // given
        ErrorType errorType = ErrorType.MEME_NOT_FOUND;
        Object errorData = "추가 에러 정보";

        // when
        ApiResponse<?> response = ApiResponse.error(errorType, errorData);

        // then
        then(response.resultType()).isEqualTo(ResultType.ERROR);
        then(response.data()).isNull();
        then(response.error()).isNotNull();
        then(response.error().code()).isEqualTo(errorType.getCode().name());
        then(response.error().message()).isEqualTo(errorType.getMessage());
        then(response.error().data()).isEqualTo(errorData);
    }

    @Test
    void success_메서드는_제네릭_타입을_올바르게_처리한다() {
        // given
        record TestData(String name, int value) {}
        TestData data = new TestData("test", 42);

        // when
        ApiResponse<TestData> response = ApiResponse.success(data);

        // then
        then(response.data()).isEqualTo(data);
        then(response.data().name()).isEqualTo("test");
        then(response.data().value()).isEqualTo(42);
    }

    @Test
    void 레코드는_equals와_hashCode를_자동으로_구현한다() {
        // given
        ApiResponse<String> response1 = ApiResponse.success("test");
        ApiResponse<String> response2 = ApiResponse.success("test");
        ApiResponse<String> response3 = ApiResponse.success("different");

        // then
        then(response1).isEqualTo(response2);
        then(response1).isNotEqualTo(response3);
        then(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void 레코드는_toString을_자동으로_구현한다() {
        // given
        ApiResponse<String> response = ApiResponse.success("test");

        // when
        String result = response.toString();

        // then
        then(result).contains("ApiResponse");
        then(result).contains("SUCCESS");
        then(result).contains("test");
    }
}
