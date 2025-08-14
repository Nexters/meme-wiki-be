package learning;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

public class CronExpressionTest {
    @Test
    void CronExpression_next_메서드를_확인한다() {
        CronExpression expression = CronExpression.parse("0 0 4 * * *");

        LocalDateTime next = expression.next(LocalDateTime.of(2025, 8, 14, 3, 59, 59));
        LocalDateTime tomorrow = expression.next(LocalDateTime.of(2025, 8, 14, 4, 0, 1));

        Assertions.assertThat(next).isEqualTo(LocalDateTime.of(2025, 8, 14, 4, 0, 0));
        Assertions.assertThat(tomorrow).isEqualTo(LocalDateTime.of(2025, 8, 15, 4, 0, 0));
    }
}
