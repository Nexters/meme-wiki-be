package spring.memewikibe.infrastructure.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Default query rewriter that performs light normalization only.
 * Acts as a fallback when AI-powered query rewriting is unavailable.
 * This implementation provides basic text normalization without semantic understanding.
 */
@Component
public class SimpleQueryRewriter implements QueryRewriter {

    /**
     * Performs basic normalization on the query string.
     * Trims whitespace, converts to lowercase, and collapses multiple spaces.
     *
     * @param userContext User context (currently unused in simple implementation)
     * @param query The original query string
     * @return Normalized query string, or empty string if query is null or blank
     */
    @Override
    public String rewrite(String userContext, String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    /**
     * Returns the original query without keyword expansion.
     * This simple implementation does not perform AI-based keyword extraction.
     *
     * @param query The original query string
     * @return The original query, or empty string if query is null or blank
     */
    @Override
    public String expandForKeywords(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query;
    }
}