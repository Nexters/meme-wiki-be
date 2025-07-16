package spring.memewikibe.support.response;

import java.util.List;

public class PageResponse<P extends Paging, T> {
    private final P paging;
    private final List<T> results;

    public PageResponse(P paging, List<T> results) {
        this.paging = paging;
        this.results = results;
    }

    public static <T> PageResponse<Cursor, T> cursor(Cursor paging, List<T> results) {
        return new PageResponse<>(paging, results);
    }

    public P getPaging() {
        return paging;
    }

    public List<T> getResults() {
        return results;
    }
}
