package spring.memewikibe.infrastructure.ai;

public interface QueryRewriter {

    /**
     * 사용자의 의도를 더 잘 반영하는 문장으로 재작성합니다. (기존 기능)
     * @param userContext 사용자 컨텍스트
     * @param query 원본 쿼리
     * @return 재작성된 쿼리
     */
    String rewrite(String userContext, String query);

    /**
     * 키워드 검색에 사용하기 위해 쿼리에서 핵심 키워드와 유의어를 추출하여 확장합니다. (새로운 기능)
     * @param query 원본 쿼리
     * @return 공백으로 구분된 키워드 문자열 (예: "퇴사 사직 회사")
     */
    String expandForKeywords(String query);
}