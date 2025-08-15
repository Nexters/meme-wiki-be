package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.response.QuizProblemResponse;
import spring.memewikibe.domain.meme.quiz.MemeQuiz;
import spring.memewikibe.infrastructure.MemeQuizRepository;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class QuizServiceTest {

    private final QuizService quizService;
    private final MemeQuizRepository quizRepository;

    QuizServiceTest(QuizService quizService, MemeQuizRepository quizRepository) {
        this.quizService = quizService;
        this.quizRepository = quizRepository;
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
}


