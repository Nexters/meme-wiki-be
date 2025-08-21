package spring.memewikibe.infrastructure;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLSubQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeAggregationResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static spring.memewikibe.domain.meme.QMeme.meme;
import static spring.memewikibe.domain.meme.QMemeCustomLog.memeCustomLog;
import static spring.memewikibe.domain.meme.QMemeShareLog.memeShareLog;
import static spring.memewikibe.domain.meme.QMemeViewLog.memeViewLog;

@Repository
public class MemeAggregationRepositoryImpl implements MemeAggregationRepository {

    private final JPAQueryFactory queryFactory;

    public MemeAggregationRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemeAggregationResult> findTopRatedMemesBy(Duration duration, int limit) {
        LocalDateTime aggregationPoint = LocalDateTime.now().minus(duration);

        final int CUSTOM_WEIGHT = 3;
        final int SHARE_WEIGHT = 2;
        final int VIEW_WEIGHT = 1;

        JPQLSubQuery<Long> customCount = JPAExpressions
            .select(memeCustomLog.count())
            .from(memeCustomLog)
            .where(memeCustomLog.meme.eq(meme)
                .and(memeCustomLog.createdAt.gt(aggregationPoint)));

        JPQLSubQuery<Long> shareCount = JPAExpressions
            .select(memeShareLog.count())
            .from(memeShareLog)
            .where(memeShareLog.meme.eq(meme)
                .and(memeShareLog.createdAt.gt(aggregationPoint)));

        JPQLSubQuery<Long> viewCount = JPAExpressions
            .select(memeViewLog.count())
            .from(memeViewLog)
            .where(memeViewLog.meme.eq(meme)
                .and(memeViewLog.createdAt.gt(aggregationPoint)));

        NumberTemplate<Long> customCountExpression = Expressions.numberTemplate(Long.class, "COALESCE(({0}), 0)", customCount);
        NumberTemplate<Long> shareCountExpression = Expressions.numberTemplate(Long.class, "COALESCE(({0}), 0)", shareCount);
        NumberTemplate<Long> viewCountExpression = Expressions.numberTemplate(Long.class, "COALESCE(({0}), 0)", viewCount);

        return queryFactory
            .select(Projections.constructor(MemeAggregationResult.class,
                meme.id,
                meme.title,
                meme.imgUrl,
                viewCountExpression,
                shareCountExpression,
                customCountExpression,
                customCountExpression.multiply(CUSTOM_WEIGHT)
                    .add(shareCountExpression.multiply(SHARE_WEIGHT))
                    .add(viewCountExpression.multiply(VIEW_WEIGHT))
            ))
            .from(meme)
            .where(meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(customCountExpression.multiply(CUSTOM_WEIGHT)
                    .add(shareCountExpression.multiply(SHARE_WEIGHT))
                    .add(viewCountExpression.multiply(VIEW_WEIGHT)).desc(),
                meme.id.desc())
            .limit(limit)
            .fetch();
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

    private BooleanExpression titleContains(String title) {
        return title != null && !title.trim().isEmpty() ? meme.title.containsIgnoreCase(title) : null;
    }
}
