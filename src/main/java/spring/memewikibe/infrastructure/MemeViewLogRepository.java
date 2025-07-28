package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.MemeViewLog;

public interface MemeViewLogRepository extends JpaRepository<MemeViewLog, Long> {
}
