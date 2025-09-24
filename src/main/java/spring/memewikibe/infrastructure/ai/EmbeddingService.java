package spring.memewikibe.infrastructure.ai;

public interface EmbeddingService {
    /**
     * Returns an embedding vector for the given text.
     * Implementations may call Vertex AI, Naver AI Studio, or a placeholder.
     */
    float[] embed(String text);
}
