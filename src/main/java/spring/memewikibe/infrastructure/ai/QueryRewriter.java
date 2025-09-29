package spring.memewikibe.infrastructure.ai;

/**
 * LLM-based query rewriter interface.
 * Implementations may call NAVER HyperCLOVA X or other LLMs to rewrite user queries.
 */
public interface QueryRewriter {
    /**
     * Rewrite the given user query given optional user context. Should return a normalized query string.
     */
    String rewrite(String userContext, String query);
}
