package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = {NaverRagService.class})
class NaverRagServiceTest {

    @Autowired
    private NaverRagService naverRagService;

    @Test
    @DisplayName("NAVER_AI_API_KEY 미설정 시 후보 그대로 반환한다")
    void returnsCandidatesWhenKeyMissing() {
        List<Long> input = List.of(1L, 2L, 3L);
        List<Long> out = naverRagService.recommendWithContext("user", "query", input);
        then(out).containsExactlyElementsOf(input);
    }
}
