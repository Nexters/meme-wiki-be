package spring.memewikibe.domain.meme;

import java.time.Duration;

public record PopularMemeProperties(Duration ttl, double viewScore, double customScore, double shareScore) {
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private static final double DEFAULT_VIEW_SCORE = 1.0;
    private static final double DEFAULT_CUSTOM_SCORE = 2.0;
    private static final double DEFAULT_SHARE_SCORE = 3.0;

}
