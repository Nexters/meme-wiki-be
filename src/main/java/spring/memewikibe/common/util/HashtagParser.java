package spring.memewikibe.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class HashtagParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<String> parseHashtags(String hashtagsJson) {
        if (hashtagsJson == null || hashtagsJson.trim().isEmpty()) {
            return emptyList();
        }

        try {
            return objectMapper.readValue(hashtagsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return emptyList();
        }
    }

    public static String toJson(String hashtags) {
        if (hashtags == null || hashtags.trim().isEmpty()) {
            return "[]";
        }
        try {
            List<String> hashtagList = Arrays.stream(hashtags.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            return objectMapper.writeValueAsString(hashtagList);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}