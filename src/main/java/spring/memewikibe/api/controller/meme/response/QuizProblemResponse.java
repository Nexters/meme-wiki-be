package spring.memewikibe.api.controller.meme.response;

import java.util.List;

public record QuizProblemResponse(
    String title,
    String summary,
    String image,
    List<QuizOption> questions
) {

    public record QuizOption(
        String message,
        boolean isRight
    ) {}

}
