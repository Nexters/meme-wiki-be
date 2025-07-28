package spring.memewikibe.api.controller.meme.response;

public record CategoryResponse(
    long id,
    String name,
    String imgUrl
) {
}
