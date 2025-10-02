package spring.memewikibe.infrastructure.ai;

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

    // [추가] 컴파일 에러 해결을 위해 expandForKeywords 메소드를 구현합니다.
    // 이 클래스는 AI 기능이 없으므로, 원본 쿼리를 그대로 반환하는 것이 가장 안전한 기본 동작입니다.
    @Override
    public String expandForKeywords(String query) {
        return query;
    }
}