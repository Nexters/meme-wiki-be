package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.Meme;

public interface MemeRepository extends JpaRepository<Meme, Long> {
}
