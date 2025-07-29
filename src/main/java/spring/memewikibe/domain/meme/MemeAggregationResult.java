package spring.memewikibe.domain.meme;

public record MemeAggregationResult(
    long id,
    String title,
    String imgUrl,
    long viewCount,
    long sharedCount,
    long customCount,
    long totalScore
) {

}
