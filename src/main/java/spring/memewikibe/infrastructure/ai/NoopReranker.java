package spring.memewikibe.infrastructure.ai;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Default heavy reranker implementation that preserves score order.
 * Acts as a placeholder to be replaced with a real cross-encoder later.
 */
@Component
public class NoopReranker implements MemeVectorIndexService.Reranker {
    @Override
    public List<MemeVectorIndexService.SearchHit> rerank(String query, List<MemeVectorIndexService.SearchHit> candidates) {
        return candidates.stream()
            .sorted(Comparator.comparingDouble(MemeVectorIndexService.SearchHit::score).reversed())
            .toList();
    }
}
