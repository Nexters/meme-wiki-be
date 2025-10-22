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
        validatePrompt(prompt);
        Meme candidateMeme = getMemeBy(memeId);
        if (userImg != null && !userImg.isEmpty()) {
            return editMemeImgWithUserRequestImg(prompt, candidateMeme.getImgUrl(), userImg);
        }
        return editMemeImgWithOnlyText(prompt, candidateMeme.getImgUrl());
    }

    private void validatePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
    }

    private GeneratedImagesResponse editMemeImgWithOnlyText(String prompt, String memeImgUrl) {
        GenerateContentResponse response = imageGenerator.generateImageWithExistingImage(prompt, memeImgUrl);

        List<Base64Image> images = getBase64Images(response);
        List<String> texts = extractTextsFrom(response);

        return new GeneratedImagesResponse(images, texts);
    }

    private Meme getMemeBy(Long memeId) {
        return memeRepository.findByIdAndNormalFlag(memeId)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));
    }

    private GeneratedImagesResponse editMemeImgWithUserRequestImg(String prompt, String existingMemeImgUrl, MultipartFile file) {
        try {
            Base64Image existingImage = convertUrlToBase64Image(existingMemeImgUrl);
            Base64Image userRequestImg = convertMultipartFileToBase64Image(file);
            List<Base64Image> requestImages = List.of(existingImage, userRequestImg);

            GenerateContentResponse response = imageGenerator.generateImageCombine(prompt, requestImages);

            List<Base64Image> images = getBase64Images(response);
            List<String> texts = extractTextsFrom(response);

            return new GeneratedImagesResponse(images, texts);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            throw new MemeWikiApplicationException(ErrorType.DEFAULT_ERROR);
        }
    }

    private Base64Image convertUrlToBase64Image(String imageUrl) {
        try {
            byte[] imageBytes = ImageUtils.downloadBytes(imageUrl);
            String mimeType = ImageUtils.detectMimeType(imageUrl, imageBytes);
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            return new Base64Image(mimeType, base64Data);
        } catch (IOException e) {
            log.error("Failed to convert URL to Base64 image: {}", imageUrl, e);
            throw new MemeWikiApplicationException(ErrorType.DEFAULT_ERROR);
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

    private List<String> extractTextsFrom(GenerateContentResponse response) {
        if (response.candidates() == null) {
            return List.of();
        }
        return response.candidates().stream()
            .filter(candidate -> candidate != null && candidate.content() != null)
            .map(GenerateContentResponse.Candidate::content)
            .filter(content -> content.parts() != null)
            .map(GenerateContentResponse.Content::parts)
            .flatMap(parts -> parts.stream()
                .filter(part -> part != null && part.text() != null)
                .map(GenerateContentResponse.Part::text))
            .toList();
    }

}
