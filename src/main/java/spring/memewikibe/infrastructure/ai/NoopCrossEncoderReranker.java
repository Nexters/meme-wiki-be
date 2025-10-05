package spring.memewikibe.infrastructure.ai;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default no-op Cross-Encoder reranker that preserves prior order by score.
 */
@Component
public class NoopCrossEncoderReranker implements CrossEncoderReranker {
    @Override
    public List<Long> rerank(String query, List<Candidate> candidates) {
        return candidates.stream()
            .sorted(Comparator.comparingDouble(Candidate::priorScore).reversed())
            .map(Candidate::id)
            .collect(Collectors.toList());
    }
}
