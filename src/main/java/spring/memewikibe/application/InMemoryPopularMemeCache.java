package spring.memewikibe.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import spring.memewikibe.domain.meme.PopularMemeRanking;
import spring.memewikibe.domain.meme.PopularMemeRankingConfig;

import java.util.List;

/**
 * 인기 밈 순위를 메모리에 캐싱하는 컴포넌트.
 * 실시간 점수 기반 Top K 순위를 관리하며, ID만 캐싱하고 실제 데이터는 DB 조회를 통해 가져옴.
 */
@Slf4j
@Component
public class InMemoryPopularMemeCache {

    private static final int TOP_K = 6;

    private final PopularMemeRanking ranking;

    public InMemoryPopularMemeCache() {
        this.ranking = new PopularMemeRanking(PopularMemeRankingConfig.defaultConfig());
    }

    public void onMemeViewed(Long memeId) {
        ranking.viewed(memeId);
        log.debug("Meme viewed: id={}", memeId);
    }

    public void onMemeCustomized(Long memeId) {
        ranking.customized(memeId);
        log.debug("Meme customized: id={}", memeId);
    }

    public void onMemeShared(Long memeId) {
        ranking.shared(memeId);
        log.debug("Meme shared: id={}", memeId);
    }

    public List<Long> getTopPopularMemeIds() {
        return ranking.getTopMemes(TOP_K);
    }

    public void initializeWithMemeIds(List<Long> memeIds) {
        for (Long memeId : memeIds) {
            ranking.viewed(memeId);
        }
        log.debug("Initialized cache with {} meme IDs", memeIds.size());
    }

    public int getTargetSize() {
        return TOP_K;
    }

}
