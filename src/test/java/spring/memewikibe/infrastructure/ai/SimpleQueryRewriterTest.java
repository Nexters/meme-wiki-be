package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
@DisplayName("SimpleQueryRewriter 단위 테스트")
class SimpleQueryRewriterTest {

    private SimpleQueryRewriter queryRewriter;

    @BeforeEach
    void setUp() {
        queryRewriter = new SimpleQueryRewriter();
    }

    @Test
    @DisplayName("rewrite: 일반 쿼리를 정규화한다")
    void rewrite_normalizesRegularQuery() {
        // given
        String query = "Hello World";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    @DisplayName("rewrite: 여러 공백을 하나로 축소한다")
    void rewrite_collapsesMultipleSpaces() {
        // given
        String query = "Hello    World   Test";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("hello world test");
    }

    @Test
    @DisplayName("rewrite: 앞뒤 공백을 제거한다")
    void rewrite_trimsWhitespace() {
        // given
        String query = "  Hello World  ";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    @DisplayName("rewrite: null 입력 시 빈 문자열을 반환한다")
    void rewrite_returnsEmptyStringForNull() {
        // when
        String result = queryRewriter.rewrite(null, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rewrite: 빈 문자열 입력 시 빈 문자열을 반환한다")
    void rewrite_returnsEmptyStringForEmpty() {
        // when
        String result = queryRewriter.rewrite(null, "");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rewrite: 공백만 있는 입력 시 빈 문자열을 반환한다")
    void rewrite_returnsEmptyStringForBlank() {
        // when
        String result = queryRewriter.rewrite(null, "   ");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rewrite: 한글 쿼리를 정규화한다")
    void rewrite_normalizesKoreanQuery() {
        // given
        String query = "안녕하세요  세계";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("안녕하세요 세계");
    }

    @Test
    @DisplayName("rewrite: 특수문자를 보존한다")
    void rewrite_preservesSpecialCharacters() {
        // given
        String query = "Hello! @World #Test";

        // when
        String result = queryRewriter.rewrite(null, query);

        // then
        assertThat(result).isEqualTo("hello! @world #test");
    }

    @Test
    @DisplayName("rewrite: userContext는 무시된다")
    void rewrite_ignoresUserContext() {
        // given
        String query = "Test Query";
        String userContext = "Some Context";

        // when
        String result = queryRewriter.rewrite(userContext, query);

        // then
        assertThat(result).isEqualTo("test query");
    }

    @Test
    @DisplayName("expandForKeywords: 원본 쿼리를 반환한다")
    void expandForKeywords_returnsOriginalQuery() {
        // given
        String query = "test query";

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo("test query");
    }

    @Test
    @DisplayName("expandForKeywords: null 입력 시 빈 문자열을 반환한다")
    void expandForKeywords_returnsEmptyStringForNull() {
        // when
        String result = queryRewriter.expandForKeywords(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("expandForKeywords: 빈 문자열 입력 시 빈 문자열을 반환한다")
    void expandForKeywords_returnsEmptyStringForEmpty() {
        // when
        String result = queryRewriter.expandForKeywords("");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("expandForKeywords: 공백만 있는 입력 시 빈 문자열을 반환한다")
    void expandForKeywords_returnsEmptyStringForBlank() {
        // when
        String result = queryRewriter.expandForKeywords("   ");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("expandForKeywords: 공백이 있는 쿼리를 보존한다")
    void expandForKeywords_preservesSpaces() {
        // given
        String query = "test query with spaces";

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo("test query with spaces");
    }

    @Test
    @DisplayName("expandForKeywords: 한글 쿼리를 보존한다")
    void expandForKeywords_preservesKoreanQuery() {
        // given
        String query = "회사 그만두다";

        // when
        String result = queryRewriter.expandForKeywords(query);

        // then
        assertThat(result).isEqualTo("회사 그만두다");
    }
}
