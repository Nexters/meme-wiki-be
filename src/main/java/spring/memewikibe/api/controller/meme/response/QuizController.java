package spring.memewikibe.api.controller.meme.response;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.meme.request.QuizAnswerRequest;
import spring.memewikibe.application.QuizService;
import spring.memewikibe.support.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public ApiResponse<List<QuizProblemResponse>> getQuizzes() {
        return ApiResponse.success(quizService.getRandomQuizzes());
    }

    @PostMapping
    public ApiResponse<QuizResultResponse> submitQuiz(
        @RequestBody QuizAnswerRequest request
    ) {
        // TODO: 퀴즈 제출 및 결과 조회
        return null;
    }

}
