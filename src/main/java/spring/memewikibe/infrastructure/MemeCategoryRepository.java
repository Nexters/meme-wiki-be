package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.MemeCategory;

import java.util.List;

public interface MemeCategoryRepository extends JpaRepository<MemeCategory, Long> {
    /**
     * NORMAL 플래그 밈 커서 페이지네이션 조회 (내림차순)
     * category가 null이면 전체 카테고리에서 조회
     * lastMemeId가 null이면 첫 페이지를 조회
     */
    @Query("SELECT mc FROM MemeCategory mc " +
        "WHERE (:category IS NULL OR mc.category = :category) " +
        "  AND (:lastMemeId IS NULL OR mc.meme.id < :lastMemeId) " +
        "  AND mc.meme.flag = spring.memewikibe.domain.meme.Meme$Flag.NORMAL " +
        "ORDER BY mc.meme.id DESC")
    List<MemeCategory> findNormalMemesWithCursor(
        @Param("category") Category category,
        @Param("lastMemeId") Long lastMemeId,
        Limit limit
    );

    /**
     * NORMAL 플래그 밈 커서 페이지네이션 조회 (Slice 버전)
     * category가 null이면 전체 카테고리에서 조회
     * lastMemeId가 null이면 첫 페이지를 조회
     * Slice가 자동으로 hasNext를 판단 (limit+1 조회 후 hasNext 계산)
     */
    @Query("SELECT mc FROM MemeCategory mc " +
        "WHERE (:category IS NULL OR mc.category = :category) " +
        "  AND (:lastMemeId IS NULL OR mc.meme.id < :lastMemeId) " +
        "  AND mc.meme.flag = spring.memewikibe.domain.meme.Meme$Flag.NORMAL " +
        "ORDER BY mc.meme.id DESC")
    Slice<MemeCategory> findNormalMemesWithCursorAsSlice(
        @Param("category") Category category,
        @Param("lastMemeId") Long lastMemeId,
        Pageable pageable
    );

    /**
     * 특정 밈의 카테고리 연결 정보를 조회합니다.
     */
    List<MemeCategory> findByMemeId(Long memeId);

    /**
     * 특정 밈의 모든 카테고리 연결을 삭제합니다.
     */
    @Modifying
    @Transactional
    void deleteByMemeId(Long memeId);
}
