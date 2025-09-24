package spring.memewikibe.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Lightweight, dependency-free placeholder embedding.
 *
 * NOTE: Replace with real Vertex AI or Naver AI Studio embedding calls in production.
 */
@Slf4j
@Service
public class DefaultEmbeddingService implements EmbeddingService {

    // Target dimension commonly used by modern embedding models (adjust if needed)
    @Value("${embedding.dimension:1536}")
    private int dimension;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            text = "";
        }
        // Deterministic pseudo-embedding: hash to seed PRNG and generate stable vector.
        byte[] hash = sha256(text);
        long seed = 0;
        for (int i = 0; i < Math.min(8, hash.length); i++) {
            seed = (seed << 8) | (hash[i] & 0xFF);
        }
        Random rnd = new Random(seed);
        float[] v = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            v[i] = (float) (rnd.nextGaussian());
        }
        // L2 normalize
        double norm = 0.0;
        for (float f : v) norm += f * f;
        norm = Math.sqrt(norm);
        if (norm == 0) return v;
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
        return v;
    }

    private static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Should not happen
            return s.getBytes(StandardCharsets.UTF_8);
        }
    }
}
