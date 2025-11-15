package spring.memewikibe.infrastructure;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLSubQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
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

    private static final int CUSTOM_WEIGHT = 3;
    private static final int SHARE_WEIGHT = 2;
    private static final int VIEW_WEIGHT = 1;
    private final JPAQueryFactory queryFactory;

    public MemeAggregationRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemeAggregationResult> findTopRatedMemesBy(Duration duration, int limit) {
        LocalDateTime aggregationPoint = LocalDateTime.now().minus(duration);

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

        NumberTemplate<Long> totalScoreExpression = Expressions.numberTemplate(Long.class,
            "({0} * {1} + {2} * {3} + {4} * {5})",
            customCountExpression, CUSTOM_WEIGHT,
            shareCountExpression, SHARE_WEIGHT,
            viewCountExpression, VIEW_WEIGHT);

        return queryFactory
            .select(Projections.constructor(MemeAggregationResult.class,
                meme.id,
                meme.title,
                meme.imgUrl,
                viewCountExpression,
                shareCountExpression,
                customCountExpression,
                totalScoreExpression
            ))
            .from(meme)
            .where(meme.flag.eq(Meme.Flag.NORMAL))
            .orderBy(totalScoreExpression.desc(), meme.id.desc())
            .limit(limit)
            .fetch();
    }


}
