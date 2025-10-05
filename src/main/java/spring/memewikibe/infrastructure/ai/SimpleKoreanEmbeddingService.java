package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default stub implementation for KoreanEmbeddingService.
 * For now, it delegates to the existing EmbeddingService implementation (Vertex/Default).
 * Later, replace this with a NAVER embedding client and keep the interface stable.
 */
@Service
@RequiredArgsConstructor
public class SimpleKoreanEmbeddingService implements KoreanEmbeddingService {

    private final EmbeddingService delegate; // existing embedding provider bean

    @Override
    public float[] embed(String text) {
        return delegate.embed(text);
    }
}
