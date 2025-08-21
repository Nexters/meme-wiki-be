package spring.memewikibe.application.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 기반 밈 추천 서비스
 * 
 * Vector Store에서 검색된 유사한 밈들을 바탕으로
 * LLM을 통해 사용자 상황에 맞는 최적의 밈을 추천합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemeRecommendationService {
    
    private final MemeEmbeddingService embeddingService;
    private final ChatClient chatClient;

    /**
     * 사용자의 상황 설명을 받아 가장 적합한 밈을 추천합니다.
     * 
     * @param userSituation 사용자가 입력한 상황 설명
     * @return LLM이 생성한 밈 추천 결과
     */
    public MemeRecommendationResult recommendMeme(String userSituation) {
        if (userSituation == null || userSituation.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 상황이 입력되지 않았습니다.");
        }
        
        log.info("Processing meme recommendation for user situation: '{}'", userSituation);
        
        try {
            // 1. Vector Store에서 유사한 밈들 검색
            List<Document> similarMemes = embeddingService.searchSimilarMemes(userSituation, 5);
            
            if (similarMemes.isEmpty()) {
                log.warn("No similar memes found for situation: '{}'", userSituation);
                return createNoResultsResponse(userSituation);
            }
            
            // 2. 검색된 밈들을 컨텍스트로 구성
            String memeContext = buildMemeContext(similarMemes);
            
            // 3. 프롬프트 생성 및 LLM 호출
            String prompt = buildRecommendationPrompt(userSituation, memeContext);
            String llmResponse = chatClient.prompt(prompt).call().content();
            
            // 디버깅용 로그 추가
            log.info("=== LLM Response ===");
            log.info(llmResponse);
            log.info("=== End LLM Response ===");
            
            // 4. LLM이 추천한 밈 선택
            Document selectedMeme = selectRecommendedMeme(llmResponse, similarMemes);
            String explanation = extractExplanation(llmResponse);
            
            // 5. 결과 구성
            MemeRecommendationResult result = MemeRecommendationResult.builder()
                    .userSituation(userSituation)
                    .recommendedMemeId((long) Double.parseDouble(selectedMeme.getMetadata().get("meme_id").toString()))
                    .recommendedMemeTitle(selectedMeme.getMetadata().get("title").toString())
                    .recommendedMemeImageUrl(selectedMeme.getMetadata().get("img_url").toString())
                    .explanation(explanation)
                    .similarMemesCount(similarMemes.size())
                    .build();
            
            log.info("Successfully generated meme recommendation for user situation");
            return result;
            
        } catch (Exception e) {
            log.error("Failed to generate meme recommendation for situation: '{}'", userSituation, e);
            throw new RuntimeException("밈 추천 생성에 실패했습니다.", e);
        }
    }

    /**
     * 검색된 밈들을 LLM 컨텍스트로 구성합니다.
     */
    private String buildMemeContext(List<Document> similarMemes) {
        return similarMemes.stream()
                .map(this::formatMemeForContext)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 개별 밈을 컨텍스트 형태로 포맷팅합니다.
     */
    private String formatMemeForContext(Document document) {
        return String.format(
                "[밈 %s]\n" +
                "제목: %s\n" +
                "유래: %s\n" +
                "사용 맥락: %s\n" +
                "해시태그: %s",
                document.getMetadata().get("meme_id"),
                document.getMetadata().get("title"),
                document.getMetadata().get("origin"),
                document.getMetadata().get("usage_context"),
                document.getMetadata().get("hashtags")
        );
    }

    /**
     * LLM에 전달할 추천 프롬프트를 생성합니다.
     */
    private String buildRecommendationPrompt(String userSituation, String memeContext) {
        return String.format("""
                너는 사용자의 상황에 딱 맞는 밈을 추천하고 그 이유를 친근하고 재치있게 설명해주는 밈 전문가야.
                
                아래에 사용자의 상황과, 그와 관련성이 높아 보이는 밈 후보들이 있어.
                
                [사용자 상황]
                %s
                
                [참고할 밈 후보들]
                %s
                
                위 정보를 바탕으로, 사용자 상황에 가장 적절한 밈 1개를 골라 추천해줘.
                
                **중요: 반드시 첫 줄에 다음 형식으로 시작해줘:**
                RECOMMENDED_MEME_ID: [선택한 밈의 ID 번호만]
                
                그 다음 줄부터는 추천 이유를 설명해줘:
                - 추천하는 밈의 제목을 먼저 언급하고
                - 그 밈이 왜 지금 상황에 딱 맞는지 구체적으로 설명해줘
                - 유래나 사용 맥락을 자연스럽게 녹여서 설명해줘
                - 공감할 수 있고 친근한 말투로 작성해줘
                - 200자 내외로 간결하게 작성해줘
                
                예시:
                RECOMMENDED_MEME_ID: 123
                선택한 밈 제목이 이 상황에 딱 맞는 이유는...
                """, userSituation, memeContext);
    }

    /**
     * 검색 결과가 없을 때의 응답을 생성합니다.
     */
    private MemeRecommendationResult createNoResultsResponse(String userSituation) {
        return MemeRecommendationResult.builder()
                .userSituation(userSituation)
                .recommendedMemeId(null)
                .recommendedMemeTitle("추천 결과 없음")
                .recommendedMemeImageUrl("")
                .explanation("죄송해요, 입력하신 상황과 딱 맞는 밈을 찾지 못했어요. 좀 더 구체적으로 상황을 설명해주시거나, 다른 표현으로 다시 시도해보세요!")
                .similarMemesCount(0)
                .build();
    }

    /**
     * LLM 응답에서 추천된 밈을 선택합니다.
     */
    private Document selectRecommendedMeme(String llmResponse, List<Document> similarMemes) {
        log.info("=== Trying to select recommended meme ===");
        log.info("Available memes: {}", similarMemes.size());
        for (int i = 0; i < similarMemes.size(); i++) {
            String memeId = similarMemes.get(i).getMetadata().get("meme_id").toString();
            String title = similarMemes.get(i).getMetadata().get("title").toString();
            log.info("  [{}] ID: {}, Title: {}", i, memeId, title);
        }
        
        // 1. ID로 찾기
        Document selectedByid = findMemeById(llmResponse, similarMemes);
        if (selectedByid != null) {
            return selectedByid;
        }
        
        // 2. 제목으로 찾기 
        Document selectedByTitle = findMemeByTitle(llmResponse, similarMemes);
        if (selectedByTitle != null) {
            return selectedByTitle;
        }
        
        // 3. 키워드로 찾기
        Document selectedByKeyword = findMemeByKeyword(llmResponse, similarMemes);
        if (selectedByKeyword != null) {
            return selectedByKeyword;
        }
        
        // 4. fallback: 가장 유사도가 높은 첫 번째 밈 (랜덤이 아닌 확정적 선택)
        log.info("⚠️ Using fallback: selecting first meme (highest similarity)");
        return similarMemes.get(0);
    }
    
    private Document findMemeById(String llmResponse, List<Document> similarMemes) {
        try {
            String firstLine = llmResponse.split("\\n")[0];
            if (firstLine.contains("RECOMMENDED_MEME_ID:")) {
                String recommendedId = firstLine.replace("RECOMMENDED_MEME_ID:", "").trim();
                log.info("🔍 LLM recommended meme ID: {}", recommendedId);
                
                for (Document meme : similarMemes) {
                    String memeId = meme.getMetadata().get("meme_id").toString();
                    String cleanMemeId = memeId.contains(".") ? memeId.split("\\.")[0] : memeId;
                    if (cleanMemeId.equals(recommendedId)) {
                        log.info("✅ Found recommended meme by ID: {}", recommendedId);
                        return meme;
                    }
                }
                log.warn("❌ Recommended meme ID {} not found in search results", recommendedId);
            }
        } catch (Exception e) {
            log.error("Error parsing meme ID from LLM response", e);
        }
        return null;
    }
    
    private Document findMemeByTitle(String llmResponse, List<Document> similarMemes) {
        try {
            // LLM 응답에서 밈 제목 언급 찾기
            for (Document meme : similarMemes) {
                String title = meme.getMetadata().get("title").toString();
                if (title != null && llmResponse.contains(title)) {
                    log.info("✅ Found recommended meme by title: {}", title);
                    return meme;
                }
            }
            log.debug("🔍 No meme found by title matching");
        } catch (Exception e) {
            log.error("Error finding meme by title", e);
        }
        return null;
    }
    
    private Document findMemeByKeyword(String llmResponse, List<Document> similarMemes) {
        try {
            // 핵심 키워드로 찾기
            String[] keywords = {"허탈", "망함", "폭망", "박명수", "무한도전", "실패"};
            
            for (String keyword : keywords) {
                if (llmResponse.contains(keyword)) {
                    for (Document meme : similarMemes) {
                        String content = meme.getText();
                        String title = meme.getMetadata().get("title").toString();
                        
                        if ((content != null && content.contains(keyword)) ||
                            (title != null && title.contains(keyword))) {
                            log.info("✅ Found recommended meme by keyword '{}': {}", keyword, title);
                            return meme;
                        }
                    }
                }
            }
            log.debug("🔍 No meme found by keyword matching");
        } catch (Exception e) {
            log.error("Error finding meme by keyword", e);
        }
        return null;
    }

    /**
     * LLM 응답에서 추천 이유 부분만 추출합니다.
     */
    private String extractExplanation(String llmResponse) {
        try {
            // 첫 번째 줄을 제거하고 나머지 텍스트 반환
            String[] lines = llmResponse.split("\\n", 2);
            if (lines.length > 1 && lines[0].contains("RECOMMENDED_MEME_ID:")) {
                return lines[1].trim();
            }
        } catch (Exception e) {
            log.error("Error extracting explanation from LLM response", e);
        }
        
        // fallback: 전체 응답 반환
        return llmResponse;
    }
}
