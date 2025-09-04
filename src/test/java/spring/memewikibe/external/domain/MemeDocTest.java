package spring.memewikibe.external.domain;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import spring.memewikibe.domain.meme.Meme;

class MemeDocTest {

    @Test
    void memeDoc은_Meme_정보를_문자열로_doc에_저장한다() {

        Meme meme = Meme.builder()
            .title("무야호")
            .origin("유재석이 무한도전에서 처음 사용")
            .usageContext("놀라거나 신날 때")
            .trendPeriod("2015년")
            .hashtags("[\"#유재석\", \"#무한도전\", \"#무야호\"]")
            .build();
        setMemeId(meme,1L);

        MemeDoc sut = MemeDoc.from(meme);

        BDDAssertions.then(sut).extracting(
            "id", "doc"
        ).containsExactlyInAnyOrder(
            "1",
            "제목: 무야호 | 기원: 유재석이 무한도전에서 처음 사용 | 사용 맥락: 놀라거나 신날 때 | 유행 시기: 2015년 | 태그: [\"#유재석\", \"#무한도전\", \"#무야호\"]"
        );
    }

    private void setMemeId(Meme meme, Long id) {
        ReflectionTestUtils.setField(meme, "id", id);
    }
}