package spring.memewikibe.common.util;

import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.assertThatThrownBy;

@UnitTest
class ImageUtilsTest {

    @Test
    void downloadBytes_빈_URL로_예외_발생() {
        // When & Then
        assertThatThrownBy(() -> ImageUtils.downloadBytes(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");
    }

    @Test
    void downloadBytes_null_URL로_예외_발생() {
        // When & Then
        assertThatThrownBy(() -> ImageUtils.downloadBytes(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");
    }

    @Test
    void downloadBytes_잘못된_URL_형식으로_예외_발생() {
        // When & Then
        assertThatThrownBy(() -> ImageUtils.downloadBytes("invalid-url"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URI");
    }

    // ===== 네트워크 독립적인 MIME 타입 감지 테스트 =====

    @Test
    void PNG_파일_시그니처로_MIME_타입_감지() {
        // Given
        byte[] pngData = createPngBytes();

        // When - 네트워크 요청이 실패하더라도 시그니처로 감지해야 함
        String result = ImageUtils.detectMimeType("file:///invalid/path.png", pngData);

        // Then
        then(result).isEqualTo("image/png");
    }

    @Test
    void JPEG_파일_시그니처로_MIME_타입_감지() {
        // Given
        byte[] jpegData = createJpegBytes();

        // When
        String result = ImageUtils.detectMimeType("file:///invalid/path.jpg", jpegData);

        // Then
        then(result).isEqualTo("image/jpeg");
    }

    @Test
    void GIF_파일_시그니처로_MIME_타입_감지() {
        // Given
        byte[] gifData = createGifBytes();

        // When
        String result = ImageUtils.detectMimeType("file:///invalid/path.gif", gifData);

        // Then
        then(result).isEqualTo("image/gif");
    }

    @Test
    void WebP_파일_시그니처로_MIME_타입_감지() {
        // Given
        byte[] webpData = createWebpBytes();

        // When
        String result = ImageUtils.detectMimeType("file:///invalid/path.webp", webpData);

        // Then
        then(result).isEqualTo("image/webp");
    }

    @Test
    void BMP_파일_시그니처로_MIME_타입_감지() {
        // Given
        byte[] bmpData = createBmpBytes();

        // When
        String result = ImageUtils.detectMimeType("file:///invalid/path.bmp", bmpData);

        // Then
        then(result).isEqualTo("image/bmp");
    }

    @Test
    void 확장자_기반_MIME_타입_감지() {
        // Given
        byte[] unknownData = {1, 2, 3, 4}; // 알 수 없는 시그니처

        // When & Then - 확장자로 감지해야 함
        then(ImageUtils.detectMimeType("file:///test.png", unknownData)).isEqualTo("image/png");
        then(ImageUtils.detectMimeType("file:///test.jpg", unknownData)).isEqualTo("image/jpeg");
        then(ImageUtils.detectMimeType("file:///test.jpeg", unknownData)).isEqualTo("image/jpeg");
        then(ImageUtils.detectMimeType("file:///test.gif", unknownData)).isEqualTo("image/gif");
        then(ImageUtils.detectMimeType("file:///test.webp", unknownData)).isEqualTo("image/webp");
        then(ImageUtils.detectMimeType("file:///test.bmp", unknownData)).isEqualTo("image/bmp");
        then(ImageUtils.detectMimeType("file:///test.svg", unknownData)).isEqualTo("image/svg+xml");
        then(ImageUtils.detectMimeType("file:///test.tiff", unknownData)).isEqualTo("image/tiff");
        then(ImageUtils.detectMimeType("file:///test.tif", unknownData)).isEqualTo("image/tiff");
        then(ImageUtils.detectMimeType("file:///test.ico", unknownData)).isEqualTo("image/x-icon");
        then(ImageUtils.detectMimeType("file:///test.unknown", unknownData)).isEqualTo("application/octet-stream");
    }

    @Test
    void 대소문자_구분_없이_확장자_처리() {
        // Given
        byte[] unknownData = {1, 2, 3, 4};

        // When & Then
        then(ImageUtils.detectMimeType("file:///test.PNG", unknownData)).isEqualTo("image/png");
        then(ImageUtils.detectMimeType("file:///test.JPG", unknownData)).isEqualTo("image/jpeg");
        then(ImageUtils.detectMimeType("file:///test.JPEG", unknownData)).isEqualTo("image/jpeg");
        then(ImageUtils.detectMimeType("file:///test.GIF", unknownData)).isEqualTo("image/gif");
        then(ImageUtils.detectMimeType("file:///test.WEBP", unknownData)).isEqualTo("image/webp");
    }

    // ===== sniffMimeType 메서드 테스트 =====

    @Test
    void sniffMimeType_파일_시그니처_감지() {
        // When & Then
        then(ImageUtils.sniffMimeType(createPngBytes(), "test.png")).isEqualTo("image/png");
        then(ImageUtils.sniffMimeType(createJpegBytes(), "test.jpg")).isEqualTo("image/jpeg");
        then(ImageUtils.sniffMimeType(createGifBytes(), "test.gif")).isEqualTo("image/gif");
        then(ImageUtils.sniffMimeType(createWebpBytes(), "test.webp")).isEqualTo("image/webp");
        then(ImageUtils.sniffMimeType(createBmpBytes(), "test.bmp")).isEqualTo("image/bmp");
    }

    @Test
    void sniffMimeType_확장자_기반_감지() {
        // Given
        byte[] unknownData = {1, 2, 3, 4};

        // When & Then
        then(ImageUtils.sniffMimeType(unknownData, "test.png")).isEqualTo("image/png");
        then(ImageUtils.sniffMimeType(unknownData, "test.svg")).isEqualTo("image/svg+xml");
        then(ImageUtils.sniffMimeType(unknownData, null)).isEqualTo("application/octet-stream");
    }

    @Test
    void 빈_데이터_처리() {
        // When & Then
        then(ImageUtils.sniffMimeType(new byte[0], "test.png")).isEqualTo("image/png");
        then(ImageUtils.sniffMimeType(null, "test.png")).isEqualTo("image/png");
    }

    @Test
    void 짧은_데이터_처리() {
        // Given - 시그니처보다 짧은 데이터
        byte[] shortData = {1, 2};

        // When & Then
        then(ImageUtils.sniffMimeType(shortData, "test.png")).isEqualTo("image/png");
        then(ImageUtils.sniffMimeType(shortData, "test.unknown")).isEqualTo("application/octet-stream");
    }

    @Test
    void 파일_시그니처_우선순위_확인() {
        // Given - PNG 시그니처를 가진 데이터이지만 jpg 확장자
        byte[] pngData = createPngBytes();

        // When
        String result = ImageUtils.detectMimeType("file:///test.jpg", pngData);

        // Then - 시그니처가 우선되어 PNG로 감지되어야 함
        then(result).isEqualTo("image/png");
    }

    @Test
    void 모든_지원_이미지_확장자_테스트() {
        // Given
        byte[] unknownData = {1, 2, 3, 4};

        // When & Then - 모든 지원 확장자 테스트
        then(ImageUtils.detectMimeType("file:///file.png", unknownData)).isEqualTo("image/png");
        then(ImageUtils.detectMimeType("file:///file.jpg", unknownData)).isEqualTo("image/jpeg");
        then(ImageUtils.detectMimeType("file:///file.jpeg", unknownData)).isEqualTo("image/jpeg");
        then(ImageUtils.detectMimeType("file:///file.gif", unknownData)).isEqualTo("image/gif");
        then(ImageUtils.detectMimeType("file:///file.webp", unknownData)).isEqualTo("image/webp");
        then(ImageUtils.detectMimeType("file:///file.bmp", unknownData)).isEqualTo("image/bmp");
        then(ImageUtils.detectMimeType("file:///file.svg", unknownData)).isEqualTo("image/svg+xml");
        then(ImageUtils.detectMimeType("file:///file.tiff", unknownData)).isEqualTo("image/tiff");
        then(ImageUtils.detectMimeType("file:///file.tif", unknownData)).isEqualTo("image/tiff");
        then(ImageUtils.detectMimeType("file:///file.ico", unknownData)).isEqualTo("image/x-icon");
    }

    @Test
    void 파일_시그니처_정확성_검증() {
        // When & Then - 각 파일 시그니처가 정확한지 테스트
        byte[] pngBytes = createPngBytes();
        then(pngBytes[0]).isEqualTo((byte) 0x89);
        then(pngBytes[1]).isEqualTo((byte) 0x50);
        then(pngBytes[2]).isEqualTo((byte) 0x4E);
        then(pngBytes[3]).isEqualTo((byte) 0x47);

        byte[] jpegBytes = createJpegBytes();
        then(jpegBytes[0]).isEqualTo((byte) 0xFF);
        then(jpegBytes[1]).isEqualTo((byte) 0xD8);
        then(jpegBytes[2]).isEqualTo((byte) 0xFF);

        byte[] gifBytes = createGifBytes();
        then(gifBytes[0]).isEqualTo((byte) 0x47); // G
        then(gifBytes[1]).isEqualTo((byte) 0x49); // I
        then(gifBytes[2]).isEqualTo((byte) 0x46); // F
        then(gifBytes[3]).isEqualTo((byte) 0x38); // 8

        byte[] webpBytes = createWebpBytes();
        then(webpBytes[8]).isEqualTo((byte) 0x57);  // W
        then(webpBytes[9]).isEqualTo((byte) 0x45);  // E
        then(webpBytes[10]).isEqualTo((byte) 0x42); // B
        then(webpBytes[11]).isEqualTo((byte) 0x50); // P

        byte[] bmpBytes = createBmpBytes();
        then(bmpBytes[0]).isEqualTo((byte) 0x42); // B
        then(bmpBytes[1]).isEqualTo((byte) 0x4D); // M
    }

    // ===== Helper Methods =====

    /**
     * PNG 파일 시그니처를 가진 테스트 데이터 생성
     * PNG 시그니처: 89 50 4E 47 0D 0A 1A 0A
     */
    private byte[] createPngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    }

    /**
     * JPEG 파일 시그니처를 가진 테스트 데이터 생성
     * JPEG 시그니처: FF D8 FF
     */
    private byte[] createJpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
    }

    /**
     * GIF 파일 시그니처를 가진 테스트 데이터 생성
     * GIF 시그니처: 47 49 46 38 (GIF8)
     */
    private byte[] createGifBytes() {
        return new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00, 0x00};
    }

    /**
     * WebP 파일 시그니처를 가진 테스트 데이터 생성
     * WebP 시그니처: RIFF????WEBP (8-11번째 바이트가 WEBP)
     */
    private byte[] createWebpBytes() {
        return new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50, 0x00, 0x00};
    }

    /**
     * BMP 파일 시그니처를 가진 테스트 데이터 생성
     * BMP 시그니처: 42 4D (BM)
     */
    private byte[] createBmpBytes() {
        return new byte[]{0x42, 0x4D, 0x00, 0x00};
    }
}