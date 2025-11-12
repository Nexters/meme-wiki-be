package spring.memewikibe.api.controller.admin.response;

import java.util.List;
import java.util.Objects;

public record MemePopularityListResponse(
    List<MemePopularityResponse> popularMemes,
    String period,
    int totalCount
) {

    public static MemePopularityListResponse of(List<MemePopularityResponse> popularMemes, String period) {
        Objects.requireNonNull(popularMemes, "popularMemes must not be null");
        Objects.requireNonNull(period, "period must not be null");

        return new MemePopularityListResponse(
            popularMemes,
            period,
            popularMemes.size()
        );
    }
}