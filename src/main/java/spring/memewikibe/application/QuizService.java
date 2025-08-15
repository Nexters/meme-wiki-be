package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.memewikibe.api.controller.meme.request.QuizAnswerRequest;
import spring.memewikibe.api.controller.meme.response.QuizProblemResponse;
import spring.memewikibe.api.controller.meme.response.QuizResultResponse;
import spring.memewikibe.domain.meme.quiz.MemeQuiz;
import spring.memewikibe.infrastructure.MemeQuizRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private static final int quizLimit = 10;

    private final MemeQuizRepository quizRepository;

    public List<QuizProblemResponse> getRandomQuizzes() {
        return quizRepository.findRandomQuizzes(quizLimit).stream()
            .map(this::toProblem)
            .toList();
    }

    private QuizProblemResponse toProblem(MemeQuiz q) {
        int ans = q.getAnswer();
        return new QuizProblemResponse(
            q.getQuestion(),
            q.getImageUrl(),
            List.of(
                new QuizProblemResponse.QuizOption(q.getOption1(), ans == 1),
                new QuizProblemResponse.QuizOption(q.getOption2(), ans == 2),
                new QuizProblemResponse.QuizOption(q.getOption3(), ans == 3),
                new QuizProblemResponse.QuizOption(q.getOption4(), ans == 4)
            )
        );
    }
}


