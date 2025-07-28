package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.MemeCustomLog;

public interface MemeCustomLogRepository extends JpaRepository<MemeCustomLog, Long> {
}
