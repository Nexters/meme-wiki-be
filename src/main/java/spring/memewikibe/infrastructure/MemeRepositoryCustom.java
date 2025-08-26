package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;

public interface MemeRepositoryCustom {
    List<Meme> findByTitleDynamicContainingOrderByIdDesc(String title, Limit limit);

    List<Meme> findByTitleDynamicContainingAndIdLessThanOrderByIdDesc(String title, Long lastId, Limit limit);
}