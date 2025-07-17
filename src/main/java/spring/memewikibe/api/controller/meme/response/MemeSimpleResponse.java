package spring.memewikibe.api.controller.meme.response;

import java.util.Objects;

public record MemeSimpleResponse(
    long id,
    String title,
    String summary,
    String image
) {
    public MemeSimpleResponse {
        Objects.requireNonNull(title, "제목은 필수 입니다.");
    }
}