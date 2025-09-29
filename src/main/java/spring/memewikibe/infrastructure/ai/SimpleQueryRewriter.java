package spring.memewikibe.infrastructure.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Default query rewriter that performs light normalization only.
 * Later, replace with a NAVER LLM-backed implementation.
 */
@Component
public class SimpleQueryRewriter implements QueryRewriter {
    @Override
    public String rewrite(String userContext, String query) {
        if (query == null) return "";
        // Light rewrite: trim + lower + collapse spaces; keep original language tokens.
        String s = query.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
