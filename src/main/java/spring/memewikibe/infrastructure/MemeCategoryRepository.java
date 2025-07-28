package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;

import java.util.List;

public interface MemeCategoryRepository extends JpaRepository<MemeCategory, Long> {
    List<MemeCategory> findByCategoryAndMemeGreaterThanOrderByMemeAsc(Category category, Meme meme, Limit limit);

    List<MemeCategory> findByCategory(Category category, Limit limit);
}
