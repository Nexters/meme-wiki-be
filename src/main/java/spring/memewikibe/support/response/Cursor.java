package spring.memewikibe.support.response;

public class Cursor implements Paging {
    private final long next;
    private final boolean hasMore;
    private final int pageSize;

    public Cursor(long next, boolean hasMore, int pageSize) {
        this.next = next;
        this.hasMore = hasMore;
        this.pageSize = pageSize;
    }

    public long getNext() {
        return next;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getPageSize() {
        return pageSize;
    }
}
