package spring.memewikibe.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.api.controller.image.response.GeneratedImagesResponse;
import spring.memewikibe.common.util.ImageUtils;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class ImageEditService {

    private final ImageGenerator imageGenerator;
    private final MemeRepository memeRepository;

    public ImageEditService(ImageGenerator imageGenerator, MemeRepository memeRepository) {
        this.imageGenerator = imageGenerator;
        this.memeRepository = memeRepository;
    }

    public GeneratedImagesResponse editMemeImg(String prompt, Long memeId, MultipartFile userImg) {
        Meme candidateMeme = getMemeBy(memeId);
        if (userImg != null && !userImg.isEmpty()) {
            return editMemeImgWithWithUserRequestImg(prompt, candidateMeme.getImgUrl(), userImg);
        }
        return editMemeImgWithOnlyText(prompt, candidateMeme.getImgUrl());
    }

    private GeneratedImagesResponse editMemeImgWithOnlyText(String prompt, String memeImgUrl) {
        GenerateContentResponse response = imageGenerator.generateImageWithExistingImage(prompt, memeImgUrl);
        List<Base64Image> images = getBase64Images(response);

        return new GeneratedImagesResponse(images);
    }

    private Meme getMemeBy(Long memeId) {
        return memeRepository.findByIdAndNormalFlag(memeId)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));
    }

    private GeneratedImagesResponse editMemeImgWithWithUserRequestImg(String prompt, String existingMemeImgUrl, MultipartFile file) {
        try {
            Base64Image existingImage = convertUrlToBase64Image(existingMemeImgUrl);
            Base64Image userRequestImg = convertMultipartFileToBase64Image(file);
            List<Base64Image> requestImages = List.of(existingImage, userRequestImg);

            GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, requestImages);
            List<Base64Image> images = getBase64Images(response);

            return new GeneratedImagesResponse(images);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    private Base64Image convertUrlToBase64Image(String imageUrl) {
        try {
            byte[] imageBytes = ImageUtils.downloadBytes(imageUrl);
            String mimeType = ImageUtils.detectMimeType(imageUrl, imageBytes);
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            return new Base64Image(mimeType, base64Data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert URL to Base64 image: " + imageUrl, e);
        }
    }

    private Base64Image convertMultipartFileToBase64Image(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String mimeType = file.getContentType();
        if (!StringUtils.hasText(mimeType)) {
            mimeType = ImageUtils.sniffMimeType(bytes, file.getOriginalFilename());
        }
        String base64Data = Base64.getEncoder().encodeToString(bytes);
        return new Base64Image(mimeType, base64Data);
    }


    private List<Base64Image> getBase64Images(GenerateContentResponse response) {
        List<Base64Image> images = new ArrayList<>();
        if (response.candidates() != null) {
            for (GenerateContentResponse.Candidate c : response.candidates()) {
                if (c == null || c.content() == null || c.content().parts() == null) continue;
                for (GenerateContentResponse.Part p : c.content().parts()) {
                    if (p != null && p.inlineData() != null) {
                        images.add(new Base64Image(p.inlineData().mimeType(), p.inlineData().data()));
                    }
                }
            }
        }
        return images;
    }

}
