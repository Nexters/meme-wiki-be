package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCustomLog;
import spring.memewikibe.domain.meme.MemeShareLog;
import spring.memewikibe.domain.meme.MemeViewLog;
import spring.memewikibe.domain.meme.event.MemeViewedEvent;
import spring.memewikibe.infrastructure.MemeCustomLogRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;
import spring.memewikibe.infrastructure.MemeViewLogRepository;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.util.function.Consumer;
import java.util.function.Function;

import static spring.memewikibe.support.error.ErrorType.MEME_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class MemeAggregationServiceImpl implements MemeAggregationService {

    private final MemeCustomLogRepository memeCustomLogRepository;
    private final MemeViewLogRepository memeViewLogRepository;
    private final MemeShareLogRepository memeShareLogRepository;
    private final MemeRepository memeRepository;
    private final InMemoryPopularMemeCache inMemoryPopularMemeCache;

    @Override
    @Transactional
    public void increaseMemeViewCount(Long memeId) {
        recordMemeActivity(memeId, MemeViewLog::of, memeViewLogRepository::save, inMemoryPopularMemeCache::onMemeViewed);
    }

    @Override
    @Transactional
    public void increaseMakeCustomMemeCount(Long memeId) {
        recordMemeActivity(memeId, MemeCustomLog::of, memeCustomLogRepository::save, inMemoryPopularMemeCache::onMemeCustomized);
    }

    @Override
    @Transactional
    public void increaseShareMemeCount(Long memeId) {
        recordMemeActivity(memeId, MemeShareLog::of, memeShareLogRepository::save, inMemoryPopularMemeCache::onMemeShared);
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMemeViewed(MemeViewedEvent event) {
        increaseMemeViewCount(event.memeId());
    }

    private <T> void recordMemeActivity(
        Long memeId,
        Function<Meme, T> logFactory,
        Consumer<T> logSaver,
        Consumer<Long> cacheUpdater
    ) {
        Meme meme = getMemeBy(memeId);
        T log = logFactory.apply(meme);
        logSaver.accept(log);
        cacheUpdater.accept(memeId);
    }

    private Meme getMemeBy(Long memeId) {
        return memeRepository.findById(memeId).orElseThrow(() -> new MemeWikiApplicationException(MEME_NOT_FOUND));
    }
}
