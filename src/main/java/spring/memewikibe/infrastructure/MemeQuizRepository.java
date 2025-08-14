package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import spring.memewikibe.domain.meme.quiz.MemeQuiz;

import java.util.List;

@Repository
public interface MemeQuizRepository extends JpaRepository<MemeQuiz, Long> {
    @Query(value = "SELECT * FROM meme_quiz ORDER BY RAND() LIMIT ?", nativeQuery = true)
    List<MemeQuiz> findRandomQuizzes(int limit);
}


