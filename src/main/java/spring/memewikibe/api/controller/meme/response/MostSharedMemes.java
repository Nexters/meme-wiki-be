package spring.memewikibe.api.controller.meme.response;

import java.time.LocalDateTime;
import java.util.List;

public record MostSharedMemes(
    List<MemeSimpleResponse> memes,
    LocalDateTime nextFetchTime
) {
}
