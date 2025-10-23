package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
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
     * 특정 카테고리의 NORMAL 플래그 밈만 조회 (내림차순)
     */
    @Query("SELECT mc FROM MemeCategory mc WHERE mc.category = :category AND mc.meme.flag = spring.memewikibe.domain.meme.Meme$Flag.NORMAL ORDER BY mc.meme.id DESC")
    List<MemeCategory> findByCategoryAndMemeNormalFlagOrderByMemeIdDesc(@Param("category") Category category, Limit limit);

    /**
     * 특정 카테고리의 NORMAL 플래그 밈만 커서 페이지네이션으로 조회 (내림차순)
     */
    @Query("SELECT mc FROM MemeCategory mc WHERE mc.category = :category AND mc.meme.id < :lastMemeId AND mc.meme.flag = spring.memewikibe.domain.meme.Meme$Flag.NORMAL ORDER BY mc.meme.id DESC")
    List<MemeCategory> findByCategoryAndMemeIdLessThanAndMemeNormalFlagOrderByMemeIdDesc(
        @Param("category") Category category,
        @Param("lastMemeId") Long lastMemeId,
        Limit limit
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
