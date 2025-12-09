package spring.memewikibe.domain.meme;

/**
 * Domain projection for simple meme information.
 * Contains only the essential fields needed for list views and recommendations.
 */
public record MemeSimpleInfo(
    long id,
    String title,
    String imgUrl
) {
}
