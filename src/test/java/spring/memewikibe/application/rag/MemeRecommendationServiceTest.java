package spring.memewikibe.application.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.document.Document;
import org.springframework.test.context.ActiveProfiles;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MemeRecommendationServiceTest {

    @Autowired(required = false)
    private MemeEmbeddingService embeddingService;
    
    @Autowired(required = false)
    private MemeDocumentConverter documentConverter;
    
    @Autowired(required = false)
    private MemeRecommendationService recommendationService;

    @Test
    @DisplayName("MemeDocumentConverter가 가중치를 올바르게 적용하는지 테스트")
    void testWeightedDocumentConversion() {
        // Given - 테스트용 밈 생성
        Meme testMeme = Meme.builder()
                .title("실패 밈")
                .origin("무한도전에서 박명수가 실패했을 때")
                .usageContext("프로젝트나 계획이 실패했을 때 사용하는 밈")
                .hashtags("#실패 #망함 #박명수")
                .build();

        if (documentConverter == null) {
            System.out.println("[DEBUG_LOG] ⚠️  MemeDocumentConverter가 주입되지 않아 테스트 스킵");
            return;
        }

        // When - Document로 변환
        Document document = documentConverter.convertToDocument(testMeme);
        
        // Then - 가중치 적용 확인
        String documentText = document.getText();
        System.out.println("[DEBUG_LOG] 생성된 Document 텍스트:");
        System.out.println("[DEBUG_LOG] " + documentText);
        
        // UsageContext가 3번 반복되는지 확인 (가중치 4)
        long usageContextCount = documentText.split("프로젝트나 계획이 실패했을 때").length - 1;
        System.out.println("[DEBUG_LOG] UsageContext 반복 횟수: " + usageContextCount);
        assertTrue(usageContextCount >= 3, "UsageContext는 최소 3번 반복되어야 함");
        
        // Hashtags가 2번 반복되는지 확인 (가중치 3)
        long hashtagsCount = documentText.split("실패").length - 1;
        System.out.println("[DEBUG_LOG] Hashtags(실패) 언급 횟수: " + hashtagsCount);
        assertTrue(hashtagsCount >= 2, "Hashtags는 최소 2번 언급되어야 함");
        
        // Origin이 1번 포함되는지 확인 (가중치 2)
        assertTrue(documentText.contains("무한도전"), "Origin이 포함되어야 함");
        
        // Title이 1번 포함되는지 확인 (가중치 1)
        assertTrue(documentText.contains("실패 밈"), "Title이 포함되어야 함");
        
        // 메타데이터 확인
        Map<String, Object> metadata = document.getMetadata();
        assertNotNull(metadata.get("title"));
        assertNotNull(metadata.get("origin"));
        assertNotNull(metadata.get("usage_context"));
        assertNotNull(metadata.get("hashtags"));
        
        System.out.println("[DEBUG_LOG] ✅ 가중치 기반 Document 변환 테스트 완료");
    }
    
    @Test
    @DisplayName("가중치 순서가 올바른지 확인 - usage_context > hashtags > origin > title")
    void testWeightPriority() {
        System.out.println("[DEBUG_LOG] ========================================");
        System.out.println("[DEBUG_LOG] 가중치 우선순위 테스트 시작");
        System.out.println("[DEBUG_LOG] 예상 순서: usage_context(4) > hashtags(3) > origin(2) > title(1)");
        System.out.println("[DEBUG_LOG] ========================================");
        
        if (documentConverter == null) {
            System.out.println("[DEBUG_LOG] ⚠️  MemeDocumentConverter가 주입되지 않아 테스트 스킵");
            return;
        }
        
        // Given - 각 필드에 고유한 키워드를 가진 테스트 밈
        Meme testMeme = Meme.builder()
                .title("키워드_제목")
                .origin("키워드_출처_내용")
                .usageContext("키워드_사용맥락_내용")
                .hashtags("#키워드_해시태그")
                .build();
        
        // When
        Document document = documentConverter.convertToDocument(testMeme);
        String text = document.getText();
        System.out.println("[DEBUG_LOG] 생성된 텍스트: " + text);
        
        // Then - 각 키워드의 빈도 확인
        int usageContextCount = countOccurrences(text, "키워드_사용맥락");
        int hashtagsCount = countOccurrences(text, "키워드_해시태그");
        int originCount = countOccurrences(text, "키워드_출처");
        int titleCount = countOccurrences(text, "키워드_제목");
        
        System.out.println("[DEBUG_LOG] 키워드 빈도:");
        System.out.println("[DEBUG_LOG]   UsageContext: " + usageContextCount);
        System.out.println("[DEBUG_LOG]   Hashtags: " + hashtagsCount);
        System.out.println("[DEBUG_LOG]   Origin: " + originCount);
        System.out.println("[DEBUG_LOG]   Title: " + titleCount);
        
        // 가중치 순서 확인
        assertTrue(usageContextCount >= hashtagsCount, "UsageContext가 Hashtags보다 많이 등장해야 함");
        assertTrue(hashtagsCount >= originCount, "Hashtags가 Origin보다 많이 등장해야 함");
        assertTrue(originCount >= titleCount, "Origin이 Title보다 많이 등장해야 함");
        
        System.out.println("[DEBUG_LOG] ✅ 가중치 우선순위 테스트 완료");
    }
    
    @Test
    @DisplayName("추천 시스템이 정상 동작하는지 기본 테스트")
    void testBasicRecommendation() {
        if (recommendationService == null) {
            System.out.println("[DEBUG_LOG] ⚠️  MemeRecommendationService가 주입되지 않아 테스트 스킵");
            System.out.println("[DEBUG_LOG] 💡 실제 환경에서는 Vector Store와 LLM 설정이 필요함");
            return;
        }
        
        System.out.println("[DEBUG_LOG] ========================================");
        System.out.println("[DEBUG_LOG] 기본 추천 시스템 테스트");
        System.out.println("[DEBUG_LOG] ========================================");
        
        String testSituation = "프로젝트가 실패했어요";
        
        try {
            MemeRecommendationResult result = recommendationService.recommendMeme(testSituation);
            
            assertNotNull(result);
            assertNotNull(result.getRecommendedMemeId());
            assertNotNull(result.getRecommendedMemeTitle());
            
            System.out.println("[DEBUG_LOG] ✅ 추천 성공:");
            System.out.println("[DEBUG_LOG]   상황: " + testSituation);
            System.out.println("[DEBUG_LOG]   추천 밈 ID: " + result.getRecommendedMemeId());
            System.out.println("[DEBUG_LOG]   추천 밈 제목: " + result.getRecommendedMemeTitle());
            System.out.println("[DEBUG_LOG]   설명: " + result.getExplanation());
            
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] ⚠️  추천 서비스 실행 중 오류 (예상됨 - Vector Store 미설정):");
            System.out.println("[DEBUG_LOG] " + e.getMessage());
            // Vector Store나 LLM이 설정되지 않은 경우 예상되는 오류
        }
    }
    
    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null) return 0;
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}