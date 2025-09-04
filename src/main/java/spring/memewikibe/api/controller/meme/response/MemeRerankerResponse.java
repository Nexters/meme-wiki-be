package spring.memewikibe.api.controller.meme.response;

import spring.memewikibe.external.domain.MemeDoc;

import java.util.List;

public record MemeRerankerResponse(
    RerankerOutcome outcome,
    String result,
    List<MemeDoc> documents,
    List<String> suggestedQueries
) {
    enum RerankerOutcome {
        SUCCESS,
        FAILURE
    }

    public static MemeRerankerResponse success(String result, List<MemeDoc> documents, List<String> suggestedQueries) {
        return new MemeRerankerResponse(RerankerOutcome.SUCCESS, result, documents, suggestedQueries);
    }

    public static MemeRerankerResponse failure(String result, List<MemeDoc> documents, List<String> suggestedQueries) {
        return new MemeRerankerResponse(RerankerOutcome.FAILURE, result, documents, suggestedQueries);
    }


}
