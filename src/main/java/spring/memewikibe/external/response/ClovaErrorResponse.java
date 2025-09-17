package spring.memewikibe.external.response;

public record ClovaErrorResponse(
    ClovaErrorStatus status,
    Object result
) {

    public record ClovaErrorStatus(
        String code,
        String message
    ) {
    }
}