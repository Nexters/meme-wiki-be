package spring.memewikibe.infrastructure.ai;

import java.util.List;

/**
 * Cross-Encoder reranker interface.
 * Implementations may call an external model to score query-document pairs and return ordered ids.
 */
public interface CrossEncoderReranker {
    record Candidate(long id, String title, String usageContext, String hashtags, double priorScore) {}

    /**
     * Reranks the given candidates for the query and returns ids in descending relevance order.
     */
    List<Long> rerank(String query, List<Candidate> candidates);
}
