package spring.memewikibe.api.controller.meme.response;

import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;
import java.util.Objects;

public record MemeDetailResponse(
    long id,
    String title,
    String usageContext,
    String origin,
    String trendPeriod,
    String imgUrl,
    List<String> hashtags
) {
    public MemeDetailResponse {
        Objects.requireNonNull(title, "제목은 필수입니다.");
    }

    public static MemeDetailResponse from(Meme meme) {
        return new MemeDetailResponse(
            meme.getId(),
            meme.getTitle(),
            meme.getUsageContext(),
            meme.getOrigin(),
            meme.getTrendPeriod(),
            meme.getImgUrl(),
            HashtagParser.parseHashtags(meme.getHashtags())
        );
    }
}
