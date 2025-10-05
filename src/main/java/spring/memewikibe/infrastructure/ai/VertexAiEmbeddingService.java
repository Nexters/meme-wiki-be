package spring.memewikibe.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Production-grade embedding service using Google Vertex AI text-embedding-004.
 *
 * - If required environment variables are not present or any call fails,
 *   this service falls back to DefaultEmbeddingService.
 * - Token is cached until just before expiry to avoid frequent auth calls.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class VertexAiEmbeddingService implements EmbeddingService {

    private final DefaultEmbeddingService fallback; // fallback when not configured or on error

    @Value("${VERTEX_AI_PROJECT_ID:}")
    private String projectId;

    @Value("${VERTEX_AI_LOCATION:us-central1}")
    private String location;

    @Value("${VERTEX_AI_EMBEDDING_MODEL:text-embedding-004}")
    private String modelName;

    @Value("${GOOGLE_CLIENT_EMAIL:}")
    private String clientEmail;

    @Value("${GOOGLE_PRIVATE_KEY:}")
    private String privateKeyPem;

    private final HttpClient http = HttpClient.newHttpClient();

    private volatile String cachedAccessToken = null;
    private volatile long tokenExpiryMillis = 0L; // epoch millis

    @Override
    public float[] embed(String text) {
        if (!isConfigured()) {
            return fallback.embed(text);
        }
        try {
            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                log.warn("Vertex token not available; falling back to default embedding");
                return fallback.embed(text);
            }

            String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
                location, urlEncode(projectId), location, urlEncode(modelName));

            String payload = "{" +
                "\"instances\":[{\"content\":\"" + escapeJson(text == null ? "" : text) + "\"}]" +
                "}";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                float[] v = parseEmbeddingValues(resp.body());
                if (v != null && v.length > 0) {
                    return v;
                }
                log.warn("Vertex embedding response parsed with 0 values; falling back");
                return fallback.embed(text);
            } else {
                log.warn("Vertex predict failed: {} - {}", resp.statusCode(), resp.body());
                return fallback.embed(text);
            }
        } catch (Exception e) {
            log.warn("Vertex predict exception; falling back: {}", e.toString());
            return fallback.embed(text);
        }
    }

    private boolean isConfigured() {
        return notBlank(projectId) && notBlank(location) && notBlank(modelName)
            && notBlank(clientEmail) && notBlank(privateKeyPem);
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && (now + 60_000) < tokenExpiryMillis) { // 60s safety margin
            return cachedAccessToken;
        }
        try {
            String assertion = buildJwtAssertion();
            String body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" +
                URLEncoder.encode(assertion, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String json = resp.body();
                String token = extractJsonString(json, "access_token");
                long expiresInSec = extractJsonLong(json, "expires_in", 3600L);
                if (token != null) {
                    cachedAccessToken = token;
                    tokenExpiryMillis = System.currentTimeMillis() + (expiresInSec - 60) * 1000; // 60s margin
                    return cachedAccessToken;
                }
            } else {
                log.warn("Google token endpoint returned {}: {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("Failed to get Google access token: {}", e.toString());
        }
        return null;
    }

    private String buildJwtAssertion() throws Exception {
        long now = Instant.now().getEpochSecond();
        long exp = now + 3600; // 1 hour
        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{" +
            "\"iss\":\"" + escapeJson(clientEmail) + "\"," +
            "\"scope\":\"https://www.googleapis.com/auth/cloud-platform\"," +
            "\"aud\":\"https://oauth2.googleapis.com/token\"," +
            "\"iat\":" + now + "," +
            "\"exp\":" + exp +
            "}";
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        byte[] sig = signRs256(signingInput.getBytes(StandardCharsets.UTF_8), parsePrivateKey(privateKeyPem));
        String signature = base64Url(sig);
        return signingInput + "." + signature;
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String norm = pem == null ? "" : pem.trim();
        if (norm.startsWith("\"") && norm.endsWith("\"")) {
            norm = norm.substring(1, norm.length() - 1);
        }
        norm = norm.replace("\\n", "\n");
        norm = norm.replace("-----BEGIN PRIVATE KEY-----", "");
        norm = norm.replace("-----END PRIVATE KEY-----", "");
        norm = norm.replaceAll("\n", "").trim();
        byte[] der = Base64.getDecoder().decode(norm);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static byte[] signRs256(byte[] data, PrivateKey key) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(data);
        return signature.sign();
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String urlEncode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractJsonString(String json, String key) {
        int pos = json.indexOf('"' + key + '"');
        if (pos < 0) return null;
        int colon = json.indexOf(':', pos);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private static long extractJsonLong(String json, String key, long defVal) {
        int pos = json.indexOf('"' + key + '"');
        if (pos < 0) return defVal;
        int colon = json.indexOf(':', pos);
        if (colon < 0) return defVal;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        int start = i;
        while (i < json.length() && (Character.isDigit(json.charAt(i)))) i++;
        if (start == i) return defVal;
        try {
            return Long.parseLong(json.substring(start, i));
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    private static float[] parseEmbeddingValues(String json) {
        // Find "values":[ ... ] and parse floats
        int keyPos = json.indexOf("\"values\"");
        if (keyPos < 0) return null;
        int colon = json.indexOf(':', keyPos);
        if (colon < 0) return null;
        int lb = json.indexOf('[', colon);
        int rb = json.indexOf(']', lb);
        if (lb < 0 || rb < 0) return null;
        String inner = json.substring(lb + 1, rb);
        String[] parts = inner.split(",");
        float[] out = new float[parts.length];
        int n = 0;
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            try {
                out[n++] = Float.parseFloat(s);
            } catch (NumberFormatException ignored) {}
        }
        if (n != out.length) {
            float[] resized = new float[n];
            System.arraycopy(out, 0, resized, 0, n);
            return resized;
        }
        return out;
    }
}
