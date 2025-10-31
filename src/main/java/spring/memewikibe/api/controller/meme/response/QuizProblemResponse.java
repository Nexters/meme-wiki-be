package spring.memewikibe.api.controller.meme.response;

import java.util.List;

public record QuizProblemResponse(
    String question,
    String image,
    List<QuizOption> options
) {

    public record QuizOption(
        String message,
        boolean isRight
    ) {}

}
