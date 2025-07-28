package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
