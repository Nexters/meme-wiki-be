package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;

public interface MemeRepository extends JpaRepository<Meme, Long>, MemeAggregationRepository {
    List<Meme> findAllByOrderByIdDesc();
}
