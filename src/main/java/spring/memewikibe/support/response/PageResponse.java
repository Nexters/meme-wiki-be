package spring.memewikibe.support.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageResponse<P extends Paging, T> {
    private final P paging;
    private final List<T> results;

    public static <T> PageResponse<Cursor, T> cursor(Cursor paging, List<T> results) {
        return new PageResponse<>(paging, results.subList(0, Math.min(results.size(), paging.getPageSize())));
    }
}
