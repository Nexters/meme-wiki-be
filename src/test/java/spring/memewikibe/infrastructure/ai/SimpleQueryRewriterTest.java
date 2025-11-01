package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimpleQueryRewriter 단위 테스트")
class SimpleQueryRewriterTest {

    private SimpleQueryRewriter queryRewriter;

    @BeforeEach
    void setUp() {
        queryRewriter = new SimpleQueryRewriter();
    }

    @Test
    @DisplayName("정상적인 쿼리를 소문자로 정규화한다")
    void rewrite_normalQuery_returnsNormalized() {
        // given
        String query = "회사 그만두고 싶다";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("회사 그만두고 싶다");
    }

    @Test
    @DisplayName("대소문자가 섞인 영문 쿼리를 소문자로 변환한다")
    void rewrite_mixedCaseQuery_returnsLowerCase() {
        // given
        String query = "Hello World TEST";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("hello world test");
    }

    @Test
    @DisplayName("여러 공백을 하나로 축약한다")
    void rewrite_multipleSpaces_returnsCollapsed() {
        // given
        String query = "너무    많은   공백";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("너무 많은 공백");
    }

    @Test
    @DisplayName("앞뒤 공백을 제거한다")
    void rewrite_withLeadingAndTrailingSpaces_returnsTrimmed() {
        // given
        String query = "  앞뒤 공백  ";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("앞뒤 공백");
    }

    @Test
    @DisplayName("null 쿼리에 대해 빈 문자열을 반환한다")
    void rewrite_nullQuery_returnsEmptyString() {
        // when
        String result = queryRewriter.rewrite(null, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 쿼리에 대해 빈 문자열을 반환한다")
    void rewrite_emptyQuery_returnsEmptyString() {
        // when
        String result = queryRewriter.rewrite(null, "");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 쿼리에 대해 빈 문자열을 반환한다")
    void rewrite_blankQuery_returnsEmptyString() {
        // when
        String result = queryRewriter.rewrite(null, "   ");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("expandForKeywords는 rewrite와 동일하게 동작한다")
    void expandForKeywords_normalQuery_returnsSameAsRewrite() {
        // given
        String query = "Hello  World";

        // when
        String rewriteResult = queryRewriter.rewrite(null, query);
        String expandResult = queryRewriter.expandForKeywords(query);

        // then
        assertThat(expandResult).isEqualTo(rewriteResult);
        assertThat(expandResult).isEqualTo("hello world");
    }

    @Test
    @DisplayName("expandForKeywords는 null 쿼리에 대해 빈 문자열을 반환한다")
    void expandForKeywords_nullQuery_returnsEmptyString() {
        // when
        String result = queryRewriter.expandForKeywords(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특수문자가 포함된 쿼리도 정규화한다")
    void rewrite_withSpecialCharacters_preservesSpecialChars() {
        // given
        String query = "회사!!  그만두고@싶다??";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("회사!! 그만두고@싶다??");
    }

    @Test
    @DisplayName("userContext는 무시되고 쿼리만 처리된다")
    void rewrite_withUserContext_ignoresContext() {
        // given
        String query = "TEST Query";
        String userContext = "user:123";

        // when
        String result = queryRewriter.rewrite(userContext, query);

        // then
        assertThat(result).isEqualTo("test query");
    }
}
