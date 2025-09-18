package spring.memewikibe.infrastructure;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;

import static spring.memewikibe.domain.meme.QMeme.meme;

@Repository
public class MemeRepositoryCustomImpl implements MemeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemeRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Meme> findByTitleOrHashtagsContainingOrderByIdDesc(String title, Limit limit) {
        return queryFactory
            .selectFrom(meme)
            .where(titleOrHashtagsContains(title), meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(meme.id.desc())
            .limit(limit.max())
            .fetch();
    }

    @Override
    public List<Meme> findByTitleOrHashtagsContainingAndIdLessThanOrderByIdDesc(String title, Long lastId, Limit limit) {
        return queryFactory
            .selectFrom(meme)
            .where(titleOrHashtagsContains(title), meme.id.lt(lastId), meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(meme.id.desc())
            .limit(limit.max())
            .fetch();
    }

    @Override
    public List<MemeSimpleResponse> findLatestMemesExcludingIds(List<Long> excludeIds, int limit) {
        return queryFactory
            .select(Projections.constructor(MemeSimpleResponse.class,
                meme.id,
                meme.title,
                meme.imgUrl
            ))
            .from(meme)
            .where(excludeIds.isEmpty() ? null : meme.id.notIn(excludeIds), meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(meme.id.desc())
            .limit(limit)
            .fetch();
    }

    private BooleanExpression titleOrHashtagsContains(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        return meme.title.containsIgnoreCase(query)
            .or(meme.hashtags.containsIgnoreCase(query));
    }
}
