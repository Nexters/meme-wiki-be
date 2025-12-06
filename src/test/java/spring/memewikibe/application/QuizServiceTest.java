package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import spring.memewikibe.annotation.IntegrationTest;
import spring.memewikibe.api.controller.meme.response.QuizProblemResponse;
import spring.memewikibe.domain.meme.quiz.MemeQuiz;
import spring.memewikibe.infrastructure.MemeQuizRepository;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.BDDAssertions.then;

@IntegrationTest
class QuizServiceTest {

    private final QuizService quizService;
    private final MemeQuizRepository quizRepository;

    QuizServiceTest(QuizService quizService, MemeQuizRepository quizRepository) {
        this.quizService = quizService;
        this.quizRepository = quizRepository;
    }

    @AfterEach
    void tearDown() {
        quizRepository.deleteAllInBatch();
    }

    @Test
    void 랜덤_퀴즈_10개를_반환한다() {
        List<MemeQuiz> quizzes = IntStream.rangeClosed(1, 20)
            .mapToObj(i -> MemeQuiz.builder()
                .question("문제 " + i)
                .option1("보기1")
                .option2("보기2")
                .option3("보기3")
                .option4("보기4")
                .answer(1)
                .imageUrl("https://example.com/" + i + ".jpg")
                .build())
            .toList();
        quizRepository.saveAll(quizzes);

        List<QuizProblemResponse> result = quizService.getRandomQuizzes();

        then(result).hasSize(10);
        result.forEach(q -> then(q.questions()).hasSize(4));
    }

    @Test
    void 퀴즈가_10개_미만일_때_존재하는_모든_퀴즈를_반환한다() {
        List<MemeQuiz> quizzes = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> MemeQuiz.builder()
                .question("문제 " + i)
                .option1("보기1")
                .option2("보기2")
                .option3("보기3")
                .option4("보기4")
                .answer(2)
                .imageUrl("https://example.com/" + i + ".jpg")
                .build())
            .toList();
        quizRepository.saveAll(quizzes);

        List<QuizProblemResponse> result = quizService.getRandomQuizzes();

        then(result).hasSize(5);
        result.forEach(q -> then(q.questions()).hasSize(4));
    }

    @Test
    void 퀴즈가_정확히_10개일_때_모든_퀴즈를_반환한다() {
        List<MemeQuiz> quizzes = IntStream.rangeClosed(1, 10)
            .mapToObj(i -> MemeQuiz.builder()
                .question("문제 " + i)
                .option1("보기1")
                .option2("보기2")
                .option3("보기3")
                .option4("보기4")
                .answer(3)
                .imageUrl("https://example.com/" + i + ".jpg")
                .build())
            .toList();
        quizRepository.saveAll(quizzes);

        List<QuizProblemResponse> result = quizService.getRandomQuizzes();

        then(result).hasSize(10);
        result.forEach(q -> then(q.questions()).hasSize(4));
    }

    @Test
    void 퀴즈가_없을_때_빈_리스트를_반환한다() {
        List<QuizProblemResponse> result = quizService.getRandomQuizzes();

        then(result).isEmpty();
    }

    @Test
    void 각_퀴즈의_정답이_올바르게_매핑된다() {
        MemeQuiz quiz = MemeQuiz.builder()
            .question("정답이 3번인 문제")
            .option1("오답1")
            .option2("오답2")
            .option3("정답")
            .option4("오답3")
            .answer(3)
            .imageUrl("https://example.com/test.jpg")
            .build();
        quizRepository.save(quiz);

        List<QuizProblemResponse> result = quizService.getRandomQuizzes();

        then(result).hasSize(1);
        QuizProblemResponse response = result.get(0);
        then(response.question()).isEqualTo("정답이 3번인 문제");
        then(response.image()).isEqualTo("https://example.com/test.jpg");
        then(response.questions()).hasSize(4);
        then(response.questions().get(0).isRight()).isFalse();
        then(response.questions().get(1).isRight()).isFalse();
        then(response.questions().get(2).isRight()).isTrue();
        then(response.questions().get(3).isRight()).isFalse();
    }
}


