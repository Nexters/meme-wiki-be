package spring.memewikibe.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HashtagParserTest {

    @Test
    void parseHashtags_정상적인_JSON_배열_파싱() {
        String hashtagsJson = "[\"#퀸은울지않아\", \"#이효리\", \"#화사\", \"#댄스가수유랑단\", \"#자존감\", \"#명언\", \"#걸크러쉬\"]";

        List<String> result = HashtagParser.parseHashtags(hashtagsJson);

        assertThat(result).hasSize(7);
        assertThat(result.get(0)).isEqualTo("#퀸은울지않아");
        assertThat(result.get(1)).isEqualTo("#이효리");
        assertThat(result.get(2)).isEqualTo("#화사");
        assertThat(result.get(6)).isEqualTo("#걸크러쉬");
    }

    @Test
    void parseHashtags_빈_문자열() {
        String hashtagsJson = "";

        List<String> result = HashtagParser.parseHashtags(hashtagsJson);

        assertThat(result).isEmpty();
    }

    @Test
    void parseHashtags_null_입력() {
        String hashtagsJson = null;

        List<String> result = HashtagParser.parseHashtags(hashtagsJson);

        assertThat(result).isEmpty();
    }

    @Test
    void parseHashtags_잘못된_JSON_형식() {
        String hashtagsJson = "invalid json";

        List<String> result = HashtagParser.parseHashtags(hashtagsJson);

        assertThat(result).isEmpty();
    }

    @Test
    void Json형식으로_변환한다() {
        String hashtags = "#퀸은울지않아, #이효리, #화사, #댄스가수유랑단, #자존감, #명언, #걸크러쉬";

        String result = HashtagParser.toJson(hashtags);

        assertThat(result).isEqualTo("[\"#퀸은울지않아, #이효리, #화사, #댄스가수유랑단, #자존감, #명언, #걸크러쉬\"]");
    }
}