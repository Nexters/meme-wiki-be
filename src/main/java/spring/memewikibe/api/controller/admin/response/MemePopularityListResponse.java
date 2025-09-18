package spring.memewikibe.api.controller.admin.response;

import java.util.List;

public record MemePopularityListResponse(
    List<MemePopularityResponse> popularMemes,
    String period,
    int totalCount
) {

    public static MemePopularityListResponse of(List<MemePopularityResponse> popularMemes, String period) {
        return new MemePopularityListResponse(
            popularMemes,
            period,
            popularMemes.size()
        );
    }
}