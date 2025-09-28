package spring.memewikibe.application;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.image.response.Base64Image;
import spring.memewikibe.api.controller.image.response.GeneratedImagesResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ImageEditService {

    private final ImageGenerator imageGenerator;
    private final MemeRepository memeRepository;

    public ImageEditService(ImageGenerator imageGenerator, MemeRepository memeRepository) {
        this.imageGenerator = imageGenerator;
        this.memeRepository = memeRepository;
    }

    private GeneratedImagesResponse editWithMemeId(String prompt, Long memeId) {
        Meme meme = memeRepository.findByIdAndNormalFlag(memeId)
            .orElseThrow(() -> new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND));
        String imageUrl = meme.getImgUrl();
        if (!StringUtils.hasText(imageUrl)) {
            throw new MemeWikiApplicationException(ErrorType.MEME_NOT_FOUND);
        }
        GenerateContentResponse response = imageGenerator.generateImageWithExistingImage(prompt, imageUrl);
        List<Base64Image> images = getBase64Images(response);

        return new GeneratedImagesResponse(images);
    }

    public GeneratedImagesResponse editWithMemeId(String prompt, Long memeId, MultipartFile maybeFile) {
        if (maybeFile != null && !maybeFile.isEmpty()) {
            return editWithFile(prompt, maybeFile);
        }
        return editWithMemeId(prompt, memeId);
    }

    private GeneratedImagesResponse editWithFile(String prompt, MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String mime = file.getContentType();
            if (!StringUtils.hasText(mime)) {
                mime = sniffMimeType(bytes, file.getOriginalFilename());
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            GenerateContentResponse response = imageGenerator.generateImageWithInlineBase64(prompt, mime, base64);
            List<Base64Image> images = getBase64Images(response);

            return new GeneratedImagesResponse(images);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    private static String sniffMimeType(byte[] data, String filename) throws IOException {
        try (InputStream is = new ByteArrayInputStream(data)) {
            String guessed = URLConnection.guessContentTypeFromStream(is);
            if (StringUtils.hasText(guessed)) return guessed;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".webp")) return "image/webp";
        }
        return "application/octet-stream";
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
