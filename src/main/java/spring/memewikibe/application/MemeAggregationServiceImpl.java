package spring.memewikibe.application;

import org.springframework.stereotype.Service;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCustomLog;
import spring.memewikibe.domain.meme.MemeShareLog;
import spring.memewikibe.domain.meme.MemeViewLog;
import spring.memewikibe.infrastructure.MemeCustomLogRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;
import spring.memewikibe.infrastructure.MemeViewLogRepository;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import static spring.memewikibe.support.error.ErrorType.MEME_NOT_FOUND;

@Service
public class MemeAggregationServiceImpl implements MemeAggregationService {

    private final MemeCustomLogRepository memeCustomLogRepository;
    private final MemeViewLogRepository memeViewLogRepository;
    private final MemeShareLogRepository memeShareLogRepository;
    private final MemeRepository memeRepository;

    public MemeAggregationServiceImpl(MemeCustomLogRepository memeCustomLogRepository, MemeViewLogRepository memeViewLogRepository, MemeShareLogRepository memeShareLogRepository, MemeRepository memeRepository) {
        this.memeCustomLogRepository = memeCustomLogRepository;
        this.memeViewLogRepository = memeViewLogRepository;
        this.memeShareLogRepository = memeShareLogRepository;
        this.memeRepository = memeRepository;
    }

    @Override
    public void increaseMemeViewCount(Long memeId) {
        Meme meme = getMemeBy(memeId);
        memeViewLogRepository.save(MemeViewLog.of(meme));
    }

    @Override
    public void increaseMakeCustomMemeCount(Long memeId) {
        Meme meme = getMemeBy(memeId);
        memeCustomLogRepository.save(MemeCustomLog.of(meme));
    }

    @Override
    public void increaseShareMemeCount(Long memeId) {
        Meme meme = getMemeBy(memeId);
        memeShareLogRepository.save(MemeShareLog.of(meme));
    }

    private Meme getMemeBy(Long memeId) {
        return memeRepository.findById(memeId).orElseThrow(() -> new MemeWikiApplicationException(MEME_NOT_FOUND));
    }
}
