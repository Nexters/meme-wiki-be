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
        Meme meme = getMemeBy(memeId);
        memeViewLogRepository.save(MemeViewLog.of(meme));
        inMemoryPopularMemeCache.onMemeViewed(memeId);
    }

    @Override
    @Transactional
    public void increaseMakeCustomMemeCount(Long memeId) {
        Meme meme = getMemeBy(memeId);
        memeCustomLogRepository.save(MemeCustomLog.of(meme));
        inMemoryPopularMemeCache.onMemeCustomized(memeId);
    }

    @Override
    @Transactional
    public void increaseShareMemeCount(Long memeId) {
        Meme meme = getMemeBy(memeId);
        memeShareLogRepository.save(MemeShareLog.of(meme));
        inMemoryPopularMemeCache.onMemeShared(memeId);
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMemeViewed(MemeViewedEvent event) {
        increaseMemeViewCount(event.memeId());
    }

    private Meme getMemeBy(Long memeId) {
        return memeRepository.findById(memeId).orElseThrow(() -> new MemeWikiApplicationException(MEME_NOT_FOUND));
    }
}
