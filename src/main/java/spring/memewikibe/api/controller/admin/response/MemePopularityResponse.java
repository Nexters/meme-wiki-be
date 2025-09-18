package spring.memewikibe.api.controller.admin.response;

public record MemePopularityResponse(
    long id,
    String title,
    String imgUrl,
    long viewCount,
    long shareCount,
    long customCount,
    long totalScore,
    int rank
) {

    public static MemePopularityResponse from(spring.memewikibe.domain.meme.MemeAggregationResult result, int rank) {
        return new MemePopularityResponse(
            result.id(),
            result.title(),
            result.imgUrl(),
            result.viewCount(),
            result.sharedCount(),
            result.customCount(),
            result.totalScore(),
            rank
        );
    }
}