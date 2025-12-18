package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.UnitTest;
import spring.memewikibe.infrastructure.ai.CrossEncoderReranker.Candidate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
@DisplayName("NoopCrossEncoderReranker 단위 테스트")
class NoopCrossEncoderRerankerTest {

    private NoopCrossEncoderReranker sut;

    @BeforeEach
    void setUp() {
        sut = new NoopCrossEncoderReranker();
    }

    @Test
    @DisplayName("rerank: 후보들을 priorScore 기준으로 내림차순 정렬하여 반환")
    void rerank_succeeds_sortsByPriorScoreDescending() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.5),
            new Candidate(4L, "제목4", "사용맥락4", "#태그4", 0.9)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(4L, 2L, 3L, 1L);
    }

    @Test
    @DisplayName("rerank: 동일한 priorScore를 가진 후보들의 순서 보존")
    void rerank_succeeds_preservesOrderForEqualScores() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.5),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.5),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.5)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then - stable sort preserves input order for equal scores
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("rerank: 단일 후보도 정상 처리")
    void rerank_succeeds_withSingleCandidate() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.7)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(1L);
    }

    @Test
    @DisplayName("rerank: 빈 후보 리스트도 정상 처리")
    void rerank_succeeds_withEmptyCandidates() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of();

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("rerank: null 쿼리도 정상 처리")
    void rerank_succeeds_withNullQuery() {
        // given
        String query = null;
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.3),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.8)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("rerank: 빈 쿼리도 정상 처리")
    void rerank_succeeds_withBlankQuery() {
        // given
        String query = "   ";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.4),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.6)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("rerank: 음수 priorScore도 정상 처리")
    void rerank_succeeds_withNegativePriorScores() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", -0.5),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.2),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", -0.1)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("rerank: 0.0 priorScore도 정상 처리")
    void rerank_succeeds_withZeroPriorScores() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.0),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.0),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.1)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("rerank: 매우 큰 priorScore 값도 정상 처리")
    void rerank_succeeds_withLargePriorScores() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 999.9),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 1000.0),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 999.8)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).containsExactly(2L, 1L, 3L);
    }

    @Test
    @DisplayName("rerank: 많은 수의 후보들도 정상 처리")
    void rerank_succeeds_withManyCandidates() {
        // given
        String query = "테스트 쿼리";
        List<Candidate> candidates = List.of(
            new Candidate(1L, "제목1", "사용맥락1", "#태그1", 0.1),
            new Candidate(2L, "제목2", "사용맥락2", "#태그2", 0.2),
            new Candidate(3L, "제목3", "사용맥락3", "#태그3", 0.3),
            new Candidate(4L, "제목4", "사용맥락4", "#태그4", 0.4),
            new Candidate(5L, "제목5", "사용맥락5", "#태그5", 0.5),
            new Candidate(6L, "제목6", "사용맥락6", "#태그6", 0.6),
            new Candidate(7L, "제목7", "사용맥락7", "#태그7", 0.7),
            new Candidate(8L, "제목8", "사용맥락8", "#태그8", 0.8),
            new Candidate(9L, "제목9", "사용맥락9", "#태그9", 0.9),
            new Candidate(10L, "제목10", "사용맥락10", "#태그10", 1.0)
        );

        // when
        List<Long> result = sut.rerank(query, candidates);

        // then
        assertThat(result).hasSize(10);
        assertThat(result).containsExactly(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
    }
}
