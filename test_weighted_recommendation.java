import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import spring.memewikibe.MemeWikiBeApplication;
import spring.memewikibe.application.rag.MemeRecommendationService;
import spring.memewikibe.application.rag.MemeRecommendationResult;

/**
 * 가중치 기반 밈 추천 시스템 테스트
 * usage_context > hashtags > origin > title 순서의 가중치가 제대로 작동하는지 확인
 */
public class test_weighted_recommendation {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MemeWikiBeApplication.class, args);
        MemeRecommendationService recommendationService = context.getBean(MemeRecommendationService.class);
        
        // 테스트 시나리오들 - 다양한 상황에서 적절한 밈이 추천되는지 확인
        String[] testSituations = {
            "프로젝트가 완전 망했어요. 계획대로 안되고 실패했어요.",
            "시험 공부를 열심히 했는데 결과가 좋지 않았어요.",
            "친구와 약속했는데 깜빡하고 늦었어요.",
            "회사에서 발표를 했는데 반응이 좋지 않았어요.",
            "다이어트를 시작했는데 며칠만에 포기했어요."
        };
        
        System.out.println("[DEBUG_LOG] ========================================");
        System.out.println("[DEBUG_LOG] 가중치 기반 밈 추천 시스템 테스트 시작");
        System.out.println("[DEBUG_LOG] 우선순위: usage_context(4) > hashtags(3) > origin(2) > title(1)");
        System.out.println("[DEBUG_LOG] ========================================");
        
        for (int i = 0; i < testSituations.length; i++) {
            String situation = testSituations[i];
            System.out.println(String.format("[DEBUG_LOG] \n테스트 케이스 %d: %s", i + 1, situation));
            
            try {
                MemeRecommendationResult result = recommendationService.recommendMeme(situation);
                
                System.out.println("[DEBUG_LOG] ✅ 추천된 밈:");
                System.out.println("[DEBUG_LOG]   ID: " + result.getRecommendedMemeId());
                System.out.println("[DEBUG_LOG]   제목: " + result.getRecommendedMemeTitle());
                System.out.println("[DEBUG_LOG]   설명: " + result.getExplanation());
                System.out.println("[DEBUG_LOG]   유사 밈 개수: " + result.getSimilarMemesCount());
                
                // 같은 상황으로 다시 테스트해서 다양성 확인
                System.out.println("[DEBUG_LOG] \n동일 상황 재테스트 (다양성 확인):");
                MemeRecommendationResult result2 = recommendationService.recommendMeme(situation);
                System.out.println("[DEBUG_LOG]   두 번째 추천 ID: " + result2.getRecommendedMemeId());
                System.out.println("[DEBUG_LOG]   두 번째 추천 제목: " + result2.getRecommendedMemeTitle());
                
                if (result.getRecommendedMemeId().equals(result2.getRecommendedMemeId())) {
                    System.out.println("[DEBUG_LOG]   ⚠️  동일한 밈이 추천됨 - 다양성 부족 가능");
                } else {
                    System.out.println("[DEBUG_LOG]   ✅ 다른 밈이 추천됨 - 다양성 확보");
                }
                
            } catch (Exception e) {
                System.out.println("[DEBUG_LOG] ❌ 추천 실패: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("[DEBUG_LOG] ----------------------------------------");
            
            // 테스트 간 간격
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        System.out.println("[DEBUG_LOG] 테스트 완료!");
        context.close();
    }
}