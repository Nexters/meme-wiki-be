package spring.memewikibe.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageUploadService 단위 테스트")
class ImageUploadServiceTest {

    @Mock
    private S3Client s3Client;

    private ImageUploadService imageUploadService;

    private static final String TEST_BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(s3Client, TEST_BUCKET);
    }

    @Test
    @DisplayName("uploadImage: 정상적인 JPG 이미지 업로드 성공")
    void uploadImage_succeeds_withValidJpgFile() throws IOException {
        // given
        MultipartFile file = createMockFile("test.jpg", "image/jpeg", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).startsWith("https://img.meme-wiki.net/" + TEST_BUCKET + "/");
        assertThat(url).endsWith(".jpg");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: 정상적인 PNG 이미지 업로드 성공")
    void uploadImage_succeeds_withValidPngFile() throws IOException {
        // given
        MultipartFile file = createMockFile("test.png", "image/png", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).endsWith(".png");
    }

    @Test
    @DisplayName("uploadImage: WEBP 이미지 업로드 성공")
    void uploadImage_succeeds_withValidWebpFile() throws IOException {
        // given
        MultipartFile file = createMockFile("test.webp", "image/webp", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).endsWith(".webp");
    }

    @Test
    @DisplayName("uploadImage: GIF 이미지 업로드 성공")
    void uploadImage_succeeds_withValidGifFile() throws IOException {
        // given
        MultipartFile file = createMockFile("test.gif", "image/gif", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).endsWith(".gif");
    }

    @Test
    @DisplayName("uploadImage: JPEG 확장자(대문자 혼합)도 정상 처리")
    void uploadImage_succeeds_withMixedCaseJpegExtension() throws IOException {
        // given
        MultipartFile file = createMockFile("test.JPEG", "image/jpeg", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).endsWith(".jpeg");
    }

    @Test
    @DisplayName("uploadImage: 빈 파일이면 IllegalArgumentException 발생")
    void uploadImage_throwsException_whenFileIsEmpty() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일이 비어있습니다.");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: 파일명이 null이면 IllegalArgumentException 발생")
    void uploadImage_throwsException_whenFilenameIsNull() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 이미지 형식입니다");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: 파일명이 빈 문자열이면 IllegalArgumentException 발생")
    void uploadImage_throwsException_whenFilenameIsEmpty() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("");

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 이미지 형식입니다");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: 확장자가 없으면 IllegalArgumentException 발생")
    void uploadImage_throwsException_whenFileHasNoExtension() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test");

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 확장자가 없습니다.");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: 지원하지 않는 확장자면 IllegalArgumentException 발생")
    void uploadImage_throwsException_whenExtensionNotSupported() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 이미지 형식입니다")
                .hasMessageContaining("jpg")
                .hasMessageContaining("png");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: BMP 확장자는 지원하지 않음")
    void uploadImage_throwsException_withBmpFile() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.bmp");

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 이미지 형식입니다");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: 점으로 시작하는 파일명도 확장자로 인식됨")
    void uploadImage_extractsExtension_whenFilenameStartsWithDot() throws IOException {
        // given
        MultipartFile file = createMockFile(".jpg", "image/jpeg", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("uploadImage: bucketName이 null이면 IllegalStateException 발생")
    void uploadImage_throwsException_whenBucketNameIsNull() throws IOException {
        // given
        ImageUploadService serviceWithNullBucket = new ImageUploadService(s3Client, null);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");

        // when & then
        assertThatThrownBy(() -> serviceWithNullBucket.uploadImage(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cloudflare R2 is not configured (bucket name missing)");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: bucketName이 빈 문자열이면 IllegalStateException 발생")
    void uploadImage_throwsException_whenBucketNameIsBlank() throws IOException {
        // given
        ImageUploadService serviceWithBlankBucket = new ImageUploadService(s3Client, "   ");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");

        // when & then
        assertThatThrownBy(() -> serviceWithBlankBucket.uploadImage(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cloudflare R2 is not configured (bucket name missing)");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage: S3 업로드 실패 시 RuntimeException 발생")
    void uploadImage_throwsException_whenS3UploadFails() throws IOException {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenThrow(new IOException("Network error"));

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImage(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("R2 업로드 실패")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("uploadImage: 파일명에 여러 개의 점이 있어도 마지막 점 이후를 확장자로 인식")
    void uploadImage_extractsExtension_fromFilenameWithMultipleDots() throws IOException {
        // given
        MultipartFile file = createMockFile("my.test.image.png", "image/png", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url = imageUploadService.uploadImage(file);

        // then
        assertThat(url).endsWith(".png");
    }

    @Test
    @DisplayName("uploadImage: 생성되는 URL에 UUID가 포함되어 고유성 보장")
    void uploadImage_generatesUniqueUrls() throws IOException {
        // given
        MultipartFile file1 = createMockFile("test.jpg", "image/jpeg", false);
        MultipartFile file2 = createMockFile("test.jpg", "image/jpeg", false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String url1 = imageUploadService.uploadImage(file1);
        String url2 = imageUploadService.uploadImage(file2);

        // then
        assertThat(url1).isNotEqualTo(url2);
        assertThat(url1).matches("https://img\\.meme-wiki\\.net/" + TEST_BUCKET + "/[0-9a-f-]{36}\\.jpg");
        assertThat(url2).matches("https://img\\.meme-wiki\\.net/" + TEST_BUCKET + "/[0-9a-f-]{36}\\.jpg");
    }

    // Helper method to create mock MultipartFile
    private MultipartFile createMockFile(String filename, String contentType, boolean isEmpty) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(isEmpty);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(1024L);

        try {
            InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
            when(file.getInputStream()).thenReturn(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return file;
    }
}
