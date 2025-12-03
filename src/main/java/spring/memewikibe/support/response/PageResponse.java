package spring.memewikibe.support.response;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<P extends Paging, T> {
    private final P paging;
    private final List<T> results;

    private PageResponse(P paging, List<T> results) {
        this.paging = paging;
        this.results = results;
    }

    public static <T> PageResponse<Cursor, T> cursor(Cursor paging, List<T> results) {
        return new PageResponse<>(paging, results.subList(0, Math.min(results.size(), paging.getPageSize())));
    }
}
