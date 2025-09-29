package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;

import java.util.Collections;
import java.util.List;

/**
 * Executes MySQL Full-Text Search outside of any active transaction so that
 * SQL grammar errors (e.g., on H2 which doesn't support MATCH...AGAINST)
 * do not mark the main transaction as rollback-only.
 */
@Component
@RequiredArgsConstructor
public class SafeFullTextSearchExecutor {

    private final MemeRepository memeRepository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<Meme> tryFullText(String query, int limit) {
        try {
            return memeRepository.findCandidatesByFullTextSearch(query, limit);
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }
}
