package spring.memewikibe.support.response;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <P> the type of paging metadata (e.g., Cursor)
 * @param <T> the type of items in the results list
 */
public record PageResponse<P extends Paging, T>(
    P paging,
    List<T> results
) {
    public static <T> PageResponse<Cursor, T> cursor(Cursor paging, List<T> results) {
        return new PageResponse<>(paging, results.subList(0, Math.min(results.size(), paging.getPageSize())));
    }
}
