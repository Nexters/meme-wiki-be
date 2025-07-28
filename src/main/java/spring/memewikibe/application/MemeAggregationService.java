package spring.memewikibe.application;

public interface MemeAggregationService {
    void increaseMemeViewCount(Long memeId);

    void increaseMakeCustomMemeCount(Long memeId);

    void increaseShareMemeCount(Long memeId);
}
