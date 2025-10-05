package spring.memewikibe.infrastructure.ai;

/**
 * Marker interface for Korean-specialized embedding service.
 * Implementations should return a float[] embedding for the input text.
 * This allows us to swap in NAVER embeddings later without touching callers.
 */
public interface KoreanEmbeddingService {
    float[] embed(String text);
}
