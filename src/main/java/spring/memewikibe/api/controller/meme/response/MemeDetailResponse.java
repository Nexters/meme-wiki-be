package spring.memewikibe.api.controller.meme.response;

public record MemeDetailResponse(
    Long id,
    String title,
    String summary,
    String source,
    String background,
    String image
) {
}
