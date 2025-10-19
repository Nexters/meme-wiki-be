package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    
    private final S3Client s3Client;
    private final String bucketName;

    public String uploadImage(MultipartFile file) {
        validateFile(file);

        // validateFile already ensures originalFilename is not null and has valid extension
        String fileName = generateUniqueFileName(getFileExtension(file.getOriginalFilename()));

        Optional.ofNullable(bucketName)
                .filter(name -> !name.isBlank())
                .orElseThrow(() -> new IllegalStateException("Cloudflare R2 is not configured (bucket name missing)"));

        return uploadToR2(file, fileName);
    }
    
    private String uploadToR2(MultipartFile file, String fileName) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("이미지 업로드 성공: {}", fileName);
            return generatePublicUrl(fileName);
        } catch (IOException e) {
            throw new RuntimeException("R2 업로드 실패", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isEmpty())
                .map(this::getFileExtension)
                .filter(ALLOWED_EXTENSIONS::contains)
                .orElseThrow(() -> new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다. 지원 형식: " + String.join(", ", ALLOWED_EXTENSIONS)
                ));
    }

    private String getFileExtension(String filename) {
        return Optional.of(filename.lastIndexOf('.'))
                .filter(index -> index != -1)
                .map(index -> filename.substring(index + 1).toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("파일 확장자가 없습니다."));
    }

    private String generateUniqueFileName(String extension) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension;
    }

    private String generatePublicUrl(String fileName) {
        return "https://img.meme-wiki.net/" + bucketName + "/" + fileName;
    }
} 