package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.memewikibe.domain.meme.Meme;

import java.util.List;
import java.util.Optional;

import static spring.memewikibe.domain.meme.Meme.Flag;

public interface MemeRepository extends JpaRepository<Meme, Long>, MemeRepositoryCustom {
    List<Meme> findAllByOrderByIdDesc();

    @Query("SELECT DISTINCT m, c.name FROM Meme m " +
        "LEFT JOIN MemeCategory mc ON m.id = mc.meme.id " +
        "LEFT JOIN Category c ON mc.category.id = c.id " +
        "ORDER BY m.id DESC")
    List<Object[]> findAllWithCategoryNamesOrderByIdDesc();

    // Flag별 조회 메서드
    List<Meme> findByFlagOrderByIdDesc(Flag flag);

    @Query("SELECT DISTINCT m, c.name FROM Meme m " +
        "LEFT JOIN MemeCategory mc ON m.id = mc.meme.id " +
        "LEFT JOIN Category c ON mc.category.id = c.id " +
        "WHERE m.flag = :flag " +
        "ORDER BY m.id DESC")
    List<Object[]> findByFlagWithCategoryNamesOrderByIdDesc(Flag flag);

    // 상태별 개수 조회
    long countByFlag(Flag flag);

    // 일괄 삭제 메서드
    void deleteByIdIn(List<Long> ids);

    // ID로 NORMAL 밈만 조회
    @Query("SELECT m FROM Meme m WHERE m.id = :id AND m.flag = spring.memewikibe.domain.meme.Meme$Flag.NORMAL")
    Optional<Meme> findByIdAndNormalFlag(@Param("id") Long id);

}
