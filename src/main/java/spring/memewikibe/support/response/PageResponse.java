package spring.memewikibe.support.response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 페이지네이션 응답을 나타내는 불변 클래스.
 *
 * @param <P> 페이징 정보 타입 ({@link Paging} 구현체)
 * @param <T> 결과 데이터 타입
 */
public class PageResponse<P extends Paging, T> {
    private final P paging;
    private final List<T> results;

    /**
     * 페이지 응답을 생성합니다.
     *
     * @param paging 페이징 메타데이터 (커서 정보 등)
     * @param results 조회된 결과 리스트
     * @throws NullPointerException paging 또는 results가 null인 경우
     */
    private PageResponse(P paging, List<T> results) {
        this.paging = Objects.requireNonNull(paging, "paging must not be null");
        this.results = Collections.unmodifiableList(Objects.requireNonNull(results, "results must not be null"));
    }

    /**
     * 커서 기반 페이지네이션 응답을 생성하는 팩토리 메서드.
     * <p>
     * 이 메서드는 {@code results}를 {@code paging.getPageSize()}만큼 자동으로 제한합니다.
     * {@link Cursor#of} 메서드와 함께 사용할 때, 이 메서드에 전달되는 results는
     * limit+1 크기일 수 있으므로, 이 메서드가 정확히 pageSize만큼 잘라냅니다.
     *
     * @param paging 커서 페이징 정보
     * @param results 결과 리스트 (pageSize보다 클 수 있음, 일반적으로 limit+1 크기)
     * @param <T> 결과 데이터 타입
     * @return 커서 기반 페이지 응답 (pageSize만큼 제한된 결과 포함)
     * @throws NullPointerException paging 또는 results가 null인 경우
     */
    public static <T> PageResponse<Cursor, T> cursor(Cursor paging, List<T> results) {
        Objects.requireNonNull(paging, "paging must not be null");
        Objects.requireNonNull(results, "results must not be null");
        int endIndex = Math.min(results.size(), paging.getPageSize());
        List<T> limitedResults = results.subList(0, endIndex);
        return new PageResponse<>(paging, limitedResults);
    }

    public P getPaging() {
        return paging;
    }

    public List<T> getResults() {
        return results;
    }
}
