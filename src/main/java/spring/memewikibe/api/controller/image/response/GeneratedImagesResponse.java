package spring.memewikibe.api.controller.image.response;


import java.util.List;

public record GeneratedImagesResponse(
    List<Base64Image> images,
    List<String> text
) {
}
