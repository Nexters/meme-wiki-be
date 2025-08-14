package spring.memewikibe.domain.meme.quiz;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.memewikibe.domain.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "meme_quiz")
public class MemeQuiz extends BaseEntity {

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "option_1", nullable = false, length = 255)
    private String option1;

    @Column(name = "option_2", nullable = false, length = 255)
    private String option2;

    @Column(name = "option_3", nullable = false, length = 255)
    private String option3;

    @Column(name = "option_4", nullable = false, length = 255)
    private String option4;

    @Column(name = "answer", nullable = false)
    private int answer;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder
    private MemeQuiz(String question, String option1, String option2, String option3, String option4, int answer, String imageUrl) {
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.answer = answer;
        this.imageUrl = imageUrl;
    }
}


