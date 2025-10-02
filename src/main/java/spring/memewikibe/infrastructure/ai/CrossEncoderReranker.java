package spring.memewikibe.infrastructure.ai;

import java.util.List;

public interface CrossEncoderReranker {
    record Candidate(long id, String title, String usageContext, String hashtags, double priorScore) {}

    List<Long> rerank(String query, List<Candidate> candidates);
}
