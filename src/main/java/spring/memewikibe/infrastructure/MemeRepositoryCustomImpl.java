package spring.memewikibe.infrastructure;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
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
    public List<Meme> findByTitleDynamicContainingOrderByIdDesc(String title, Limit limit) {
        return queryFactory
            .selectFrom(meme)
            .where(titleContains(title), meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(meme.id.desc())
            .limit(limit.max())
            .fetch();
    }

    @Override
    public List<Meme> findByTitleDynamicContainingAndIdLessThanOrderByIdDesc(String title, Long lastId, Limit limit) {
        return queryFactory
            .selectFrom(meme)
            .where(titleContains(title), meme.id.lt(lastId), meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(meme.id.desc())
            .limit(limit.max())
            .fetch();
    }

    private BooleanExpression titleContains(String title) {
        return title != null && !title.trim().isEmpty() ? meme.title.containsIgnoreCase(title) : null;
    }
}