package spring.memewikibe.support.response;

import lombok.Getter;
import spring.memewikibe.domain.BaseEntity;

import java.util.List;

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
        Long next = hasMore ? entities.getLast().getId() : null;

        return new Cursor(next, hasMore, actualSize);
    }

}
