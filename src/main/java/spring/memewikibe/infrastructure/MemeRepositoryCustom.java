package spring.memewikibe.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;

public interface MemeRepositoryCustom {
    List<Meme> findByTitleOrHashtagsContainingOrderByIdDesc(String title, Limit limit);

    List<Meme> findByTitleOrHashtagsContainingAndIdLessThanOrderByIdDesc(String title, Long lastId, Limit limit);

    Slice<Meme> findByTitleOrHashtagsContainingAsSlice(String title, Long lastId, Pageable pageable);

    List<Meme> findKeywordCandidatesAcrossFields(List<String> keywords, Limit limit);

    List<MemeSimpleResponse> findLatestMemesExcludingIds(List<Long> excludeIds, int limit);
}
