package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(classes = {NaverRagService.class})
@TestPropertySource(properties = "NAVER_AI_API_KEY=test-key")
class NaverRagServiceWithKeyTest {

    @Autowired
    private NaverRagService naverRagService;

    @Test
    @DisplayName("NAVER_AI_API_KEY 설정 시에도 현재는 그대로 반환 (Stub)")
    void returnsCandidatesWhenKeyPresent() {
        List<Long> input = List.of(10L, 20L);
        List<Long> out = naverRagService.recommendWithContext("user", "query", input);
        then(out).containsExactlyElementsOf(input);
    }
}
