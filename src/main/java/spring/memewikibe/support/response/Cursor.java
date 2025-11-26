package spring.memewikibe.support.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;
import spring.memewikibe.domain.BaseEntity;

import java.util.List;
import java.util.function.Function;

@Getter
public class Cursor implements Paging {
    private final Long next;
    private final boolean hasMore;
    private final int pageSize;

    public Cursor(Long next, boolean hasMore, int pageSize) {
        this.next = next;
        this.hasMore = hasMore;
        this.pageSize = pageSize;
    }

    public static Cursor of(List<? extends BaseEntity> entities, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("크기 제한은 0보다 커야 합니다.");
        }

        if (entities.isEmpty()) {
            return new Cursor(null, false, 0);
        }

        boolean hasMore = entities.size() > limit;
        int actualSize = Math.min(entities.size(), limit);
        Long next = hasMore ? entities.get(actualSize - 1).getId() : null;

        return new Cursor(next, hasMore, actualSize);
    }

    public static <T extends BaseEntity> Cursor fromSlice(Slice<T> slice) {
        if (slice.isEmpty()) {
            return new Cursor(null, false, 0);
        }

        boolean hasNext = slice.hasNext();
        int size = slice.getNumberOfElements();
        Long next = hasNext ? slice.getContent().get(size - 1).getId() : null;

        return new Cursor(next, hasNext, size);
    }

    public static <T> Cursor fromSlice(Slice<T> slice, Function<T, Long> idExtractor) {
        if (slice.isEmpty()) {
            return new Cursor(null, false, 0);
        }

        boolean hasNext = slice.hasNext();
        int size = slice.getNumberOfElements();
        Long next = hasNext ? idExtractor.apply(slice.getContent().get(size - 1)) : null;

        return new Cursor(next, hasNext, size);
    }

}
