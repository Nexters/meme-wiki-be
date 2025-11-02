package spring.memewikibe.infrastructure.ai;

/**
 * Marker interface for Korean-specialized embedding service.
 * Implementations should return a float[] embedding for the input text.
 * This allows us to swap in NAVER embeddings later without touching callers.
 *
 * <p>This interface provides semantic separation for Korean text embedding,
 * allowing future implementations to use specialized Korean language models
 * (e.g., NAVER AI Studio) without requiring changes to calling code.
 *
 * <p>Current implementation delegates to the standard {@link EmbeddingService},
 * but future versions may use Korean-optimized embedding models.
 */
public interface KoreanEmbeddingService {
    /**
     * Returns an embedding vector for the given Korean text.
     *
     * @param text the text to embed (may be null or blank)
     * @return float array representing the embedding vector
     */
    float[] embed(String text);
}
