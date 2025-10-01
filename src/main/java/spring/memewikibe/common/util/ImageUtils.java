package spring.memewikibe.common.util;

import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ImageUtils {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static byte[] downloadBytes(String url) throws IOException {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        try {
            URI uri = new URI(url);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                throw new IOException("HTTP error " + response.statusCode() + " when downloading: " + url);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    public static String detectMimeType(String imageUrl, byte[] data) {
        // Try by HTTP response header
        try {
            URI uri = new URI(imageUrl);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(5))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
            
            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            String contentType = response.headers().firstValue("Content-Type").orElse(null);
            if (StringUtils.hasText(contentType)) {
                // Content-Type might include charset, so extract only the MIME type
                return contentType.split(";")[0].trim();
            }
        } catch (Exception ignored) {
            // If HEAD request fails, continue with other methods
        }
        
        // Try by stream content sniffing using legacy method (still works for content detection)
        String guessed = guessContentTypeFromBytes(data);
        if (StringUtils.hasText(guessed)) return guessed;
        
        // Fallback by extension
        return getMimeTypeByExtension(imageUrl);
    }

    public static String sniffMimeType(byte[] data, String filename) {
        String guessed = guessContentTypeFromBytes(data);
        if (StringUtils.hasText(guessed)) return guessed;
        return getMimeTypeByExtension(filename);
    }
    
    private static String guessContentTypeFromBytes(byte[] data) {
        if (data == null || data.length < 8) {
            return null;
        }
        
        // Check common image file signatures
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x38) {
            return "image/gif";
        }
        if (data.length >= 12 && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return "image/webp";
        }
        if (data[0] == 0x42 && data[1] == 0x4D) {
            return "image/bmp";
        }
        
        return null;
    }
    
    private static String getMimeTypeByExtension(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
