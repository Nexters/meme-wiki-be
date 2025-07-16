package spring.memewikibe.api.controller.meme.response;

import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.meme.request.QuizAnswerRequest;
import spring.memewikibe.support.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    @GetMapping
    public ApiResponse<List<QuizProblemResponse>> getQuizzes() {
        // TODO: 퀴즈 목록 조회
        return null;
    }

    @PostMapping
    public ApiResponse<QuizResultResponse> submitQuiz(
        @RequestBody QuizAnswerRequest request
    ) {
        // TODO: 퀴즈 제출 및 결과 조회
        return null;
    }

}
