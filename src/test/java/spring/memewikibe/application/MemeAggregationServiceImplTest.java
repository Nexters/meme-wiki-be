package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCustomLog;
import spring.memewikibe.domain.meme.MemeShareLog;
import spring.memewikibe.domain.meme.MemeViewLog;
import spring.memewikibe.infrastructure.MemeCustomLogRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.MemeShareLogRepository;
import spring.memewikibe.infrastructure.MemeViewLogRepository;
import spring.memewikibe.support.error.ErrorType;
import spring.memewikibe.support.error.MemeWikiApplicationException;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@Transactional
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeAggregationServiceImplTest {

    private final MemeAggregationServiceImpl memeAggregationService;
    private final MemeRepository memeRepository;
    private final MemeViewLogRepository memeViewLogRepository;
    private final MemeCustomLogRepository memeCustomLogRepository;
    private final MemeShareLogRepository memeShareLogRepository;

    MemeAggregationServiceImplTest(
        MemeAggregationServiceImpl memeAggregationService,
        MemeRepository memeRepository,
        MemeViewLogRepository memeViewLogRepository,
        MemeCustomLogRepository memeCustomLogRepository,
        MemeShareLogRepository memeShareLogRepository
    ) {
        this.memeAggregationService = memeAggregationService;
        this.memeRepository = memeRepository;
        this.memeViewLogRepository = memeViewLogRepository;
        this.memeCustomLogRepository = memeCustomLogRepository;
        this.memeShareLogRepository = memeShareLogRepository;
    }

    @AfterEach
    void tearDown() {
        memeViewLogRepository.deleteAllInBatch();
        memeCustomLogRepository.deleteAllInBatch();
        memeShareLogRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("increaseMemeViewCount: 밈 조회수를 증가시킨다")
    void increaseMemeViewCount_success() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseMemeViewCount(meme.getId());

        // then
        List<MemeViewLog> logs = memeViewLogRepository.findAll();
        then(logs).hasSize(1);
        then(logs.get(0).getMeme().getId()).isEqualTo(meme.getId());
    }

    @Test
    @DisplayName("increaseMemeViewCount: 존재하지 않는 밈 ID로 요청 시 예외 발생")
    void increaseMemeViewCount_withNonExistentMemeId_throwsException() {
        // given
        Long nonExistentId = 99999L;

        // when & then
        thenThrownBy(() -> memeAggregationService.increaseMemeViewCount(nonExistentId))
            .isInstanceOf(MemeWikiApplicationException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MEME_NOT_FOUND);
    }

    @Test
    @DisplayName("increaseMemeViewCount: 여러 번 호출 시 로그가 누적된다")
    void increaseMemeViewCount_multipleInvocations_accumulatesLogs() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseMemeViewCount(meme.getId());
        memeAggregationService.increaseMemeViewCount(meme.getId());
        memeAggregationService.increaseMemeViewCount(meme.getId());

        // then
        List<MemeViewLog> logs = memeViewLogRepository.findAll();
        then(logs).hasSize(3);
        then(logs).allMatch(log -> log.getMeme().getId().equals(meme.getId()));
    }

    @Test
    @DisplayName("increaseMakeCustomMemeCount: 커스텀 밈 생성 횟수를 증가시킨다")
    void increaseMakeCustomMemeCount_success() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseMakeCustomMemeCount(meme.getId());

        // then
        List<MemeCustomLog> logs = memeCustomLogRepository.findAll();
        then(logs).hasSize(1);
        then(logs.get(0).getMeme().getId()).isEqualTo(meme.getId());
    }

    @Test
    @DisplayName("increaseMakeCustomMemeCount: 존재하지 않는 밈 ID로 요청 시 예외 발생")
    void increaseMakeCustomMemeCount_withNonExistentMemeId_throwsException() {
        // given
        Long nonExistentId = 99999L;

        // when & then
        thenThrownBy(() -> memeAggregationService.increaseMakeCustomMemeCount(nonExistentId))
            .isInstanceOf(MemeWikiApplicationException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MEME_NOT_FOUND);
    }

    @Test
    @DisplayName("increaseMakeCustomMemeCount: 여러 번 호출 시 로그가 누적된다")
    void increaseMakeCustomMemeCount_multipleInvocations_accumulatesLogs() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseMakeCustomMemeCount(meme.getId());
        memeAggregationService.increaseMakeCustomMemeCount(meme.getId());

        // then
        List<MemeCustomLog> logs = memeCustomLogRepository.findAll();
        then(logs).hasSize(2);
        then(logs).allMatch(log -> log.getMeme().getId().equals(meme.getId()));
    }

    @Test
    @DisplayName("increaseShareMemeCount: 밈 공유 횟수를 증가시킨다")
    void increaseShareMemeCount_success() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseShareMemeCount(meme.getId());

        // then
        List<MemeShareLog> logs = memeShareLogRepository.findAll();
        then(logs).hasSize(1);
        then(logs.get(0).getMeme().getId()).isEqualTo(meme.getId());
    }

    @Test
    @DisplayName("increaseShareMemeCount: 존재하지 않는 밈 ID로 요청 시 예외 발생")
    void increaseShareMemeCount_withNonExistentMemeId_throwsException() {
        // given
        Long nonExistentId = 99999L;

        // when & then
        thenThrownBy(() -> memeAggregationService.increaseShareMemeCount(nonExistentId))
            .isInstanceOf(MemeWikiApplicationException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MEME_NOT_FOUND);
    }

    @Test
    @DisplayName("increaseShareMemeCount: 여러 번 호출 시 로그가 누적된다")
    void increaseShareMemeCount_multipleInvocations_accumulatesLogs() {
        // given
        Meme meme = memeRepository.save(Meme.builder()
            .title("테스트 밈")
            .imgUrl("https://example.com/test.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseShareMemeCount(meme.getId());
        memeAggregationService.increaseShareMemeCount(meme.getId());
        memeAggregationService.increaseShareMemeCount(meme.getId());

        // then
        List<MemeShareLog> logs = memeShareLogRepository.findAll();
        then(logs).hasSize(3);
        then(logs).allMatch(log -> log.getMeme().getId().equals(meme.getId()));
    }

    @Test
    @DisplayName("서로 다른 밈에 대한 집계는 독립적으로 동작한다")
    void aggregations_forDifferentMemes_workIndependently() {
        // given
        Meme meme1 = memeRepository.save(Meme.builder()
            .title("밈 1")
            .imgUrl("https://example.com/test1.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        Meme meme2 = memeRepository.save(Meme.builder()
            .title("밈 2")
            .imgUrl("https://example.com/test2.jpg")
            .flag(Meme.Flag.NORMAL)
            .build());

        // when
        memeAggregationService.increaseMemeViewCount(meme1.getId());
        memeAggregationService.increaseMemeViewCount(meme1.getId());
        memeAggregationService.increaseMemeViewCount(meme2.getId());

        memeAggregationService.increaseMakeCustomMemeCount(meme1.getId());
        memeAggregationService.increaseMakeCustomMemeCount(meme2.getId());
        memeAggregationService.increaseMakeCustomMemeCount(meme2.getId());

        memeAggregationService.increaseShareMemeCount(meme1.getId());
        memeAggregationService.increaseShareMemeCount(meme1.getId());
        memeAggregationService.increaseShareMemeCount(meme1.getId());
        memeAggregationService.increaseShareMemeCount(meme2.getId());

        // then
        List<MemeViewLog> viewLogs = memeViewLogRepository.findAll();
        then(viewLogs).hasSize(3);
        then(viewLogs).filteredOn(log -> log.getMeme().getId().equals(meme1.getId())).hasSize(2);
        then(viewLogs).filteredOn(log -> log.getMeme().getId().equals(meme2.getId())).hasSize(1);

        List<MemeCustomLog> customLogs = memeCustomLogRepository.findAll();
        then(customLogs).hasSize(3);
        then(customLogs).filteredOn(log -> log.getMeme().getId().equals(meme1.getId())).hasSize(1);
        then(customLogs).filteredOn(log -> log.getMeme().getId().equals(meme2.getId())).hasSize(2);

        List<MemeShareLog> shareLogs = memeShareLogRepository.findAll();
        then(shareLogs).hasSize(4);
        then(shareLogs).filteredOn(log -> log.getMeme().getId().equals(meme1.getId())).hasSize(3);
        then(shareLogs).filteredOn(log -> log.getMeme().getId().equals(meme2.getId())).hasSize(1);
    }
}
