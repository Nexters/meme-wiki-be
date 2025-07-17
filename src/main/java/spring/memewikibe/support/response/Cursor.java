package spring.memewikibe.support.response;

public class Cursor implements Paging {
    private final Long next;
    private final boolean hasMore;
    private final int pageSize;

    public Cursor(Long next, boolean hasMore, int pageSize) {
        this.next = next;
        this.hasMore = hasMore;
        this.pageSize = pageSize;
    }

    public Long getNext() {
        return next;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getPageSize() {
        return pageSize;
    }
}
