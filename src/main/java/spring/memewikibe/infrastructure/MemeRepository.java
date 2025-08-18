package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;

public interface MemeRepository extends JpaRepository<Meme, Long>, MemeAggregationRepository {
    List<Meme> findAllByOrderByIdDesc();
    
    @Query("SELECT DISTINCT m, c.name FROM Meme m " +
           "LEFT JOIN MemeCategory mc ON m.id = mc.meme.id " +
           "LEFT JOIN Category c ON mc.category.id = c.id " +
           "ORDER BY m.id DESC")
    List<Object[]> findAllWithCategoryNamesOrderByIdDesc();
}
