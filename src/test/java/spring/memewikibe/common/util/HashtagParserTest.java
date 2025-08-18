package spring.memewikibe.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HashtagParserTest {

    @Test
    void parseHashtags_정상적인_JSON_배열_파싱() {
        String hashtagsJson = "[\"#퀸은울지않아\",\"#이효리\",\"#화사\",\"#댄스가수유랑단\",\"#자존감\",\"#명언\",\"#걸크러쉬\"]";

        List<String> result = HashtagParser.parseHashtags(hashtagsJson);

        assertThat(result).containsExactly(
            "#퀸은울지않아",
            "#이효리",
            "#화사",
            "#댄스가수유랑단",
            "#자존감",
            "#명언",
            "#걸크러쉬"
        );
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
    void toJson_공백_구분_문자열을_JSON배열로_변환() {
        String hashtags = "#해시태그1 #해시태그2 #해시태그3";
        String result = HashtagParser.toJson(hashtags);
        assertThat(result).isEqualTo("[\"#해시태그1\",\"#해시태그2\",\"#해시태그3\"]");
    }

    @Test
    void toJson_불필요한_공백을_처리하여_변환() {
        String hashtags = "  #해시태그1   #해시태그2  ";
        String result = HashtagParser.toJson(hashtags);
        assertThat(result).isEqualTo("[\"#해시태그1\",\"#해시태그2\"]");
    }

    @Test
    void toJson_빈_문자열() {
        String hashtags = "";
        String result = HashtagParser.toJson(hashtags);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void toJson_null_입력() {
        String hashtags = null;
        String result = HashtagParser.toJson(hashtags);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void toJson_하나의_해시태그() {
        String hashtags = "#단일태그";
        String result = HashtagParser.toJson(hashtags);
        assertThat(result).isEqualTo("[\"#단일태그\"]");
    }
}
