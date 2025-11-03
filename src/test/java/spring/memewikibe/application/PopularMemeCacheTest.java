package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

class PopularMemeCacheTest {

    private PopularMemeCache cache;

    @BeforeEach
    void setUp() {
        cache = new PopularMemeCache();
    }

    @Test
    void 초기_캐시는_비어있다() {
        // when
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then
        then(result).isEmpty();
    }

    @Test
    void onMemeViewed는_밈의_점수를_증가시킨다() {
        // given
        MemeSimpleResponse meme = new MemeSimpleResponse(1L, "무야호", "무야호.jpg");

        // when
        cache.onMemeViewed(1L, meme);
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then
        then(result).hasSize(1);
        then(result.get(0)).isEqualTo(meme);
    }

    @Test
    void onMemeCustomized는_밈의_점수를_view보다_높게_증가시킨다() {
        // given
        MemeSimpleResponse meme1 = new MemeSimpleResponse(1L, "무야호", "무야호.jpg");
        MemeSimpleResponse meme2 = new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg");

        // when
        cache.onMemeViewed(1L, meme1);
        cache.onMemeCustomized(2L, meme2);
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then - custom이 view보다 점수가 높아서 먼저 나옴
        then(result).hasSize(2);
        then(result.get(0).id()).isEqualTo(2L);  // custom
        then(result.get(1).id()).isEqualTo(1L);  // view
    }

    @Test
    void onMemeShared는_밈의_점수를_가장_높게_증가시킨다() {
        // given
        MemeSimpleResponse meme1 = new MemeSimpleResponse(1L, "무야호", "무야호.jpg");
        MemeSimpleResponse meme2 = new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg");
        MemeSimpleResponse meme3 = new MemeSimpleResponse(3L, "전남친 토스트", "전남친토스트.jpg");

        // when
        cache.onMemeViewed(1L, meme1);
        cache.onMemeCustomized(2L, meme2);
        cache.onMemeShared(3L, meme3);
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then - share > custom > view 순서
        then(result).hasSize(3);
        then(result.get(0).id()).isEqualTo(3L);  // share
        then(result.get(1).id()).isEqualTo(2L);  // custom
        then(result.get(2).id()).isEqualTo(1L);  // view
    }

    @Test
    void 동일한_밈에_여러_이벤트가_발생하면_점수가_누적된다() {
        // given
        MemeSimpleResponse meme = new MemeSimpleResponse(1L, "무야호", "무야호.jpg");

        // when
        cache.onMemeViewed(1L, meme);
        cache.onMemeViewed(1L, meme);
        cache.onMemeCustomized(1L, meme);
        cache.onMemeShared(1L, meme);
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then
        then(result).hasSize(1);
        then(result.get(0)).isEqualTo(meme);
    }

    @Test
    void Top6를_초과하는_밈은_점수가_낮은_것부터_제외된다() {
        // given
        for (int i = 1; i <= 10; i++) {
            MemeSimpleResponse meme = new MemeSimpleResponse((long) i, "meme" + i, "meme" + i + ".jpg");
            // 점수가 높을수록 나중에 추가
            for (int j = 0; j < i; j++) {
                cache.onMemeViewed((long) i, meme);
            }
        }

        // when
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then - Top 6만 반환되고, 점수가 높은 순서대로
        then(result).hasSize(6);
        then(result.get(0).id()).isEqualTo(10L);  // 점수 10
        then(result.get(1).id()).isEqualTo(9L);   // 점수 9
        then(result.get(2).id()).isEqualTo(8L);   // 점수 8
        then(result.get(3).id()).isEqualTo(7L);   // 점수 7
        then(result.get(4).id()).isEqualTo(6L);   // 점수 6
        then(result.get(5).id()).isEqualTo(5L);   // 점수 5
    }

    @Test
    void initializeWithMemes는_여러_밈으로_캐시를_초기화한다() {
        // given
        List<MemeSimpleResponse> memes = List.of(
            new MemeSimpleResponse(1L, "무야호", "무야호.jpg"),
            new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg"),
            new MemeSimpleResponse(3L, "전남친 토스트", "전남친토스트.jpg")
        );

        // when
        cache.initializeWithMemes(memes);
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then
        then(result).hasSize(3);
        then(result).containsExactlyInAnyOrderElementsOf(memes);
    }

    @Test
    void 같은_밈을_다시_보면_점수가_갱신된다() {
        // given
        MemeSimpleResponse meme1 = new MemeSimpleResponse(1L, "무야호", "무야호.jpg");
        MemeSimpleResponse meme2 = new MemeSimpleResponse(2L, "나만 아니면 돼", "나만아니면돼.jpg");

        cache.onMemeViewed(1L, meme1);
        cache.onMemeViewed(2L, meme2);

        // when - meme2를 한 번 더 봄
        cache.onMemeViewed(2L, meme2);
        List<MemeSimpleResponse> result = cache.getTopPopularMemes();

        // then - meme2가 더 높은 점수로 상위에 위치
        then(result).hasSize(2);
        then(result.get(0).id()).isEqualTo(2L);
        then(result.get(1).id()).isEqualTo(1L);
    }
}
