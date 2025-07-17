package spring.memewikibe.api.controller.meme.response;

import java.util.List;
import java.util.Objects;

public record MemeDetailResponse(
    long id,
    String title,
    String summary,
    String source,
    String background,
    String image,
    List<String> hashtags
) {
    public MemeDetailResponse {
        Objects.requireNonNull(title, "제목은 필수입니다.");
    }
}
