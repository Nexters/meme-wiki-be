package learning;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;
import spring.memewikibe.annotation.UnitTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.BDDAssertions.then;

@UnitTest
public class CronExpressionTest {
    @Test
    void CronExpression_next_메서드를_확인한다() {
        CronExpression expression = CronExpression.parse("0 0 4 * * *");

        LocalDateTime next = expression.next(LocalDateTime.of(2025, 8, 14, 3, 59, 59));
        LocalDateTime tomorrow = expression.next(LocalDateTime.of(2025, 8, 14, 4, 0, 1));

        then(next).isEqualTo(LocalDateTime.of(2025, 8, 14, 4, 0, 0));
        then(tomorrow).isEqualTo(LocalDateTime.of(2025, 8, 15, 4, 0, 0));
    }
}
