package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default stub implementation for KoreanEmbeddingService.
 * For now, it delegates to the existing EmbeddingService implementation (Vertex/Default).
 * Later, replace this with a NAVER embedding client and keep the interface stable.
 *
 * <p>This service depends on an {@link EmbeddingService} bean. By default, Spring will inject
 * the {@code @Primary} bean (VertexAiEmbeddingService), which already handles null/blank text
 * and provides fallback behavior to DefaultEmbeddingService when not configured.
 */
@Service
@RequiredArgsConstructor
public class SimpleKoreanEmbeddingService implements KoreanEmbeddingService {

    private final EmbeddingService delegate; // existing embedding provider bean

    /**
     * Returns an embedding vector for the given Korean text.
     * Delegates to the configured {@link EmbeddingService}.
     *
     * @param text the text to embed (may be null or blank)
     * @return float array representing the embedding vector
     */
    @Override
    public float[] embed(String text) {
        // Delegate handles null/blank text appropriately
        return delegate.embed(text);
    }
}
