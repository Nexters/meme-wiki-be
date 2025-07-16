package spring.memewikibe.api.controller.meme.response;

public record MemeSimpleResponse(
    Long id,
    String title,
    String summary,
    String image
) {
}