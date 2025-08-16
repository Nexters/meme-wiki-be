package spring.memewikibe.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class HashtagParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<String> parseHashtags(String hashtagsJson) {
        if (hashtagsJson == null || hashtagsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(hashtagsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public static String toJson(String hashtags) {
        if (hashtags == null || hashtags.trim().isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(Collections.singletonList(hashtags));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}