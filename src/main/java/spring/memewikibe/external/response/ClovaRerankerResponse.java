package spring.memewikibe.external.response;

import spring.memewikibe.external.domain.MemeDoc;

import java.util.List;

public record ClovaRerankerResponse(
    Status status,
    Result result
) {

    public record Status(String code, String message) {
    }

    public record Result(String result, List<MemeDoc> citedDocuments, List<String> suggestedQueries, Usage usage) {

        public record Usage(int promptTokens, int completionTokens, int totalTokens) {
        }
    }
}
