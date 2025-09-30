package spring.memewikibe.common.util;

import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ImageUtils {

    public static byte[] downloadBytes(String url) throws IOException {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        conn.setConnectTimeout(10000); // 10초 연결 타임아웃
        conn.setReadTimeout(30000);    // 30초 읽기 타임아웃
        
        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    public static String detectMimeType(String imageUrl, byte[] data) throws IOException {
        // Try by URL connection header
        try {
            URL u = new URL(imageUrl);
            URLConnection conn = u.openConnection();
            conn.setConnectTimeout(5000); // 5초 타임아웃
            String ct = conn.getContentType();
            if (StringUtils.hasText(ct)) return ct;
        } catch (Exception ignored) {
        }
        // Try by stream content sniffing
        try (InputStream is = new ByteArrayInputStream(data)) {
            String guessed = URLConnection.guessContentTypeFromStream(is);
            if (StringUtils.hasText(guessed)) return guessed;
        }
        // Fallback by extension
        return getMimeTypeByExtension(imageUrl);
    }

    public static String sniffMimeType(byte[] data, String filename) throws IOException {
        try (InputStream is = new ByteArrayInputStream(data)) {
            String guessed = URLConnection.guessContentTypeFromStream(is);
            if (StringUtils.hasText(guessed)) return guessed;
        }
        return getMimeTypeByExtension(filename);
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
        return "application/octet-stream";
    }
}
