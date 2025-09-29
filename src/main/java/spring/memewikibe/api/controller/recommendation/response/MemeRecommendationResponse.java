package spring.memewikibe.api.controller.recommendation.response;

public record MemeRecommendationResponse(
    long id,
    String title,
    String imgUrl,
    String reason
) {}
