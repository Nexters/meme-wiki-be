package spring.memewikibe.api.controller.image;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.image.response.GeneratedImagesResponse;
import spring.memewikibe.application.ImageEditService;
import spring.memewikibe.support.response.ApiResponse;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageEditService imageEditService;

    public ImageController(ImageEditService imageEditService) {
        this.imageEditService = imageEditService;
    }

    @PostMapping(value = "/edit/meme/{memeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GeneratedImagesResponse> editWithMemeMultipart(
        @PathVariable Long memeId,
        @RequestPart("prompt") String prompt,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ApiResponse.success(imageEditService.editMemeImg(prompt, memeId, image));
    }
}
