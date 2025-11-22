package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Adapter implementation for KoreanEmbeddingService that delegates to the primary EmbeddingService.
 *
 * <p>This implementation serves as a transitional adapter while Korean-specific embedding
 * capabilities are being developed. It currently delegates to VertexAiEmbeddingService
 * (or DefaultEmbeddingService as fallback) for generating embeddings.
 *
 * <p><strong>Future Enhancement:</strong> This implementation will be replaced with a
 * NAVER AI Studio embedding client optimized for Korean text. The KoreanEmbeddingService
 * interface will remain stable to avoid impacting dependent code.
 *
 * @see KoreanEmbeddingService
 * @see EmbeddingService
 */
@Service
@RequiredArgsConstructor
public class SimpleKoreanEmbeddingService implements KoreanEmbeddingService {

    /**
     * The primary embedding service to delegate to.
     * Explicitly qualified to avoid ambiguity with multiple EmbeddingService implementations.
     */
    @Qualifier("vertexAiEmbeddingService")
    private final EmbeddingService delegate;

    /**
     * Generates an embedding vector for the given Korean text.
     *
     * @param text the input text to embed (may be null or blank)
     * @return a float array representing the embedding vector
     * @throws IllegalArgumentException if text processing fails
     */
    @Override
    public float[] embed(String text) {
        return delegate.embed(text);
    }
}
