package spring.memewikibe.api.controller.image.response;

public record Base64Image(
    String mimeType,
    String data
) {}

