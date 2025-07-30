package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;

public interface MemeRepository extends JpaRepository<Meme, Long>, MemeAggregationRepository {
    List<Meme> findByTitleContainingOrderByIdDesc(String title, Limit limit);

    List<Meme> findByTitleContainingAndIdLessThanOrderByIdDesc(String title, Long id, Limit limit);
}
