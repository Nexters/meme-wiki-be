package spring.memewikibe.domain.meme;

import java.time.Duration;

public record PopularMemeRankingConfig(Duration ttl, double viewScore, double customScore, double shareScore) {
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private static final double DEFAULT_VIEW_SCORE = 1.0;
    private static final double DEFAULT_CUSTOM_SCORE = 2.0;
    private static final double DEFAULT_SHARE_SCORE = 3.0;

    public static PopularMemeRankingConfig defaultConfig() {
        return new PopularMemeRankingConfig(DEFAULT_TTL, DEFAULT_VIEW_SCORE, DEFAULT_CUSTOM_SCORE, DEFAULT_SHARE_SCORE);
    }
}
