package spring.memewikibe.external.google.client.request;

import java.util.List;

public record GenerateContentRequest(
    List<Content> contents,
    List<Tool> tools,
    SystemInstruction systemInstruction,
    GenerationConfig generationConfig,
    List<SafetySetting> safetySettings
) {
    // Convenience constructors
    public GenerateContentRequest(List<Content> contents) {
        this(contents, null, null, null, null);
    }

    public static GenerateContentRequest textOnly(String text) {
        return new GenerateContentRequest(
            List.of(new Content(
                List.of(new Part(text, null, null, null)),
                null
            ))
        );
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

    public record Tool(
        List<FunctionDeclaration> functionDeclarations
    ) {
    }

    public record FunctionDeclaration(
        String name,
        String description,
        Object schema
    ) {
    }

    public record SystemInstruction(
        List<Part> parts
    ) {
    }

    public record GenerationConfig(
        Integer temperature,
        Integer topK,
        Double topP,
        Integer candidateCount,
        Integer maxOutputTokens,
        List<String> stopSequences,
        String responseMimeType,
        Object responseSchema
    ) {
    }

    public record SafetySetting(
        String category,
        String threshold
    ) {
    }
}
