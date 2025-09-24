package spring.memewikibe.external.google.client.response;

import java.util.List;

public record GenerateContentResponse(
    List<Candidate> candidates,
    PromptFeedback promptFeedback,
    UsageMetadata usageMetadata,
    String modelVersion,
    String responseId
) {
    public record Candidate(
        Content content,
        String finishReason,
        List<SafetyRating> safetyRatings,
        Integer index
    ) {
    }

    public record Content(
        List<Part> parts,
        String role
    ) {
    }

    public record Part(
        String text,
        InlineData inlineData,
        FunctionCall functionCall,
        FunctionResponse functionResponse
    ) {
    }

    public record InlineData(
        String mimeType,
        String data
    ) {
    }

    public record FunctionCall(
        String name,
        Object args
    ) {
    }

    public record FunctionResponse(
        String name,
        Object response
    ) {
    }

    public record SafetyRating(
        String category,
        String probability,
        Boolean blocked
    ) {
    }

    public record PromptFeedback(
        List<SafetyRating> safetyRatings,
        String blockReason
    ) {
        public record SafetyRating(
            String category,
            String probability
        ) {
        }
    }

    public record UsageMetadata(
        Integer promptTokenCount,
        Integer candidatesTokenCount,
        Integer totalTokenCount,
        Integer cachedContentTokenCount
    ) {
    }
}