package spring.memewikibe.support.error;

import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import static org.assertj.core.api.BDDAssertions.then;

@UnitTest
class ErrorMessageTest {

    @Test
    void ErrorType으로_ErrorMessage를_생성한다() {
        // given
        ErrorType errorType = ErrorType.MEME_NOT_FOUND;

        // when
        ErrorMessage errorMessage = new ErrorMessage(errorType);

        // then
        then(errorMessage.getCode()).isEqualTo(errorType.getCode().name());
        then(errorMessage.getMessage()).isEqualTo(errorType.getMessage());
        then(errorMessage.getData()).isNull();
    }

    @Test
    void ErrorType과_데이터로_ErrorMessage를_생성한다() {
        // given
        ErrorType errorType = ErrorType.CATEGORY_NOT_FOUND;
        Object data = "category-123";

        // when
        ErrorMessage errorMessage = new ErrorMessage(errorType, data);

        // then
        then(errorMessage.getCode()).isEqualTo(errorType.getCode().name());
        then(errorMessage.getMessage()).isEqualTo(errorType.getMessage());
        then(errorMessage.getData()).isEqualTo(data);
    }

    @Test
    void 다양한_ErrorType을_지원한다() {
        // given
        ErrorType[] errorTypes = {
            ErrorType.DEFAULT_ERROR,
            ErrorType.MEME_NOT_FOUND,
            ErrorType.CATEGORY_NOT_FOUND,
            ErrorType.EXTERNAL_SERVICE_ERROR,
            ErrorType.EXTERNAL_SERVICE_UNAUTHORIZED,
            ErrorType.EXTERNAL_SERVICE_FORBIDDEN,
            ErrorType.EXTERNAL_SERVICE_TOO_MANY_REQUESTS,
            ErrorType.EXTERNAL_SERVICE_BAD_REQUEST
        };

        // when & then
        for (ErrorType errorType : errorTypes) {
            ErrorMessage errorMessage = new ErrorMessage(errorType);

            then(errorMessage.getCode()).isEqualTo(errorType.getCode().name());
            then(errorMessage.getMessage()).isEqualTo(errorType.getMessage());
            then(errorMessage.getData()).isNull();
        }
    }

    @Test
    void 복잡한_객체를_데이터로_포함할_수_있다() {
        // given
        ErrorType errorType = ErrorType.EXTERNAL_SERVICE_ERROR;
        record ComplexData(String field1, int field2, boolean field3) {
        }
        ComplexData complexData = new ComplexData("value", 42, true);

        // when
        ErrorMessage errorMessage = new ErrorMessage(errorType, complexData);

        // then
        then(errorMessage.getData()).isEqualTo(complexData);
        then(errorMessage.getData()).isInstanceOf(ComplexData.class);
        ComplexData data = (ComplexData) errorMessage.getData();
        then(data.field1()).isEqualTo("value");
        then(data.field2()).isEqualTo(42);
        then(data.field3()).isTrue();
    }
}
