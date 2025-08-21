package spring.memewikibe.application.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import spring.memewikibe.domain.meme.Meme;

import java.util.HashMap;
import java.util.Map;

/**
 * Meme 엔티티를 Spring AI Document로 변환하는 컨버터
 * Vector Store에 저장하기 위해 Meme의 텍스트 정보를 결합하고 메타데이터를 준비합니다.
 */
@Slf4j
@Component
public class MemeDocumentConverter {

    /**
     * Meme 엔티티를 Document로 변환합니다.
     * 
     * @param meme 변환할 Meme 엔티티
     * @return Spring AI Document 객체
     */
    public Document convertToDocument(Meme meme) {
        String combinedText = buildCombinedText(meme);
        Map<String, Object> metadata = buildMetadata(meme);
        
        // Pinecone requires unique IDs for vector upsert operations
        String documentId = "meme-" + meme.getId();
        
        log.debug("Converting meme {} to document with ID: {} and text length: {}", 
                 meme.getId(), documentId, combinedText.length());
        
        return new Document(documentId, combinedText, metadata);
    }

    /**
     * Meme의 텍스트 정보들을 가중치에 따라 결합합니다.
     * 가중치 순서: usage_context (최고) → hashtags → origin → title (최저)
     */
    private String buildCombinedText(Meme meme) {
        StringBuilder textBuilder = new StringBuilder();
        
        // 1. UsageContext (가중치 4 - 가장 중요, 3번 반복)
        if (meme.getUsageContext() != null && !meme.getUsageContext().trim().isEmpty()) {
            String usageContext = meme.getUsageContext().trim();
            textBuilder.append("상황: ").append(usageContext).append(". ");
            textBuilder.append("사용맥락: ").append(usageContext).append(". ");
            textBuilder.append("활용: ").append(usageContext).append(". ");
        }
        
        // 2. Hashtags (가중치 3 - 2번 반복)
        if (meme.getHashtags() != null && !meme.getHashtags().trim().isEmpty()) {
            String cleanedHashtags = meme.getHashtags().trim()
                    .replaceAll("#", "")
                    .replaceAll("\\s+", " ");
            textBuilder.append("키워드: ").append(cleanedHashtags).append(". ");
            textBuilder.append("태그: ").append(cleanedHashtags).append(". ");
        }
        
        // 3. Origin (가중치 2 - 1번 반복)
        if (meme.getOrigin() != null && !meme.getOrigin().trim().isEmpty()) {
            textBuilder.append("출처: ").append(meme.getOrigin().trim()).append(". ");
        }
        
        // 4. Title (가중치 1 - 1번 반복, 가장 낮은 우선순위)
        if (meme.getTitle() != null && !meme.getTitle().trim().isEmpty()) {
            textBuilder.append("제목: ").append(meme.getTitle().trim()).append(". ");
        }
        
        String result = textBuilder.toString().trim();
        log.debug("Built weighted text for meme {}: {}", meme.getId(), 
                 result.length() > 100 ? result.substring(0, 100) + "..." : result);
        
        return result;
    }

    /**
     * Vector Store에서 활용할 메타데이터를 구성합니다.
     */
    private Map<String, Object> buildMetadata(Meme meme) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("meme_id", meme.getId());
        metadata.put("title", meme.getTitle() != null ? meme.getTitle() : "");
        metadata.put("origin", meme.getOrigin() != null ? meme.getOrigin() : "");
        metadata.put("usage_context", meme.getUsageContext() != null ? meme.getUsageContext() : "");
        metadata.put("hashtags", meme.getHashtags() != null ? meme.getHashtags() : "");
        metadata.put("img_url", meme.getImgUrl() != null ? meme.getImgUrl() : "");
        metadata.put("trend_period", meme.getTrendPeriod() != null ? meme.getTrendPeriod() : "");
        
        // 검색 시 필터링에 활용할 수 있는 메타데이터 (String으로 변환)
        if (meme.getCreatedAt() != null) {
            metadata.put("created_at", meme.getCreatedAt().toString());
        }
        if (meme.getUpdatedAt() != null) {
            metadata.put("updated_at", meme.getUpdatedAt().toString());
        }
        
        return metadata;
    }
}
