package spring.memewikibe.common.util;

import java.text.Normalizer;

public final class TextNormalizer {
    private TextNormalizer() {}

    public static String normalize(String input) {
        if (input == null) return "";
        // Unicode NFKC
        String s = Normalizer.normalize(input, Normalizer.Form.NFKC);
        // Lowercase
        s = s.toLowerCase();
        // Replace hashtags separators to ensure tokens split (#tag => tag)
        s = s.replace('#', ' ');
        // Collapse whitespace
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
