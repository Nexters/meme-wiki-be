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
        if (entities.isEmpty() || limit <= 0) {
            return new Cursor(null, false, 0);
        }

        boolean hasMore = entities.size() > limit;
        int actualSize = Math.min(entities.size(), limit);
        Long next = entities.getLast().getId();

        return new Cursor(next, hasMore, actualSize);
    }

}
