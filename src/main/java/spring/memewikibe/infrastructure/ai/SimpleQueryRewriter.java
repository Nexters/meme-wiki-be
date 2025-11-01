package spring.memewikibe.infrastructure.ai;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Simple query rewriter that performs light normalization only.
 * This implementation does not use AI and provides basic text normalization.
 * Use this as a fallback when AI-powered query rewriting is not available.
 */
@Service
public class SimpleQueryRewriter implements QueryRewriter {

    @Override
    public String rewrite(String userContext, String query) {
        if (query == null) {
            return "";
        }

        if (query.isBlank()) {
            return "";
        }

        // Light rewrite: trim + lower + collapse spaces; keep original language tokens.
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    @Override
    public String expandForKeywords(String query) {
        // This simple implementation has no AI capabilities.
        // Apply the same normalization as rewrite() for consistency.
        return rewrite(null, query);
    }
}