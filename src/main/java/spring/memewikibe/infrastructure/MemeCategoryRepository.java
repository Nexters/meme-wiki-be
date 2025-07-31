package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.MemeCategory;

import java.util.List;

public interface MemeCategoryRepository extends JpaRepository<MemeCategory, Long> {
    @Query("SELECT mc FROM MemeCategory mc WHERE mc.category = :category AND mc.meme.id < :lastMemeId ORDER BY mc.meme.id DESC")
    List<MemeCategory> findByCategoryAndMemeGreaterThanOrderByMemeDesc(
        @Param("category") Category category, 
        @Param("lastMemeId") Long lastMemeId, 
        Limit limit
    );

    List<MemeCategory> findByCategory(@Param("category") Category category, Limit limit);
}
