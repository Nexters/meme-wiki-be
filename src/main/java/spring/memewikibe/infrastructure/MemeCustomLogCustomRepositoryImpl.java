package spring.memewikibe.infrastructure;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static spring.memewikibe.domain.meme.QMeme.meme;
import static spring.memewikibe.domain.meme.QMemeCustomLog.memeCustomLog;

@Repository
public class MemeCustomLogCustomRepositoryImpl implements MemeCustomLogCustomRepository {

    private final JPAQueryFactory queryFactory;

    public MemeCustomLogCustomRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemeSimpleResponse> findTopMemesByCustomCountWithin(Duration duration, int limit) {
        LocalDateTime aggregationPoint = LocalDateTime.now().minus(duration);

        return queryFactory
            .select(Projections.constructor(MemeSimpleResponse.class,
                meme.id,
                meme.title,
                meme.imgUrl
            ))
            .from(memeCustomLog)
            .join(memeCustomLog.meme, meme)
            .where(memeCustomLog.createdAt.gt(aggregationPoint))
            .groupBy(meme.id, meme.title, meme.imgUrl)
            .orderBy(memeCustomLog.count().desc(), meme.id.desc())
            .limit(limit)
            .fetch();
    }
}