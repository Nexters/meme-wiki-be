package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.meme.MemeShareLog;

public interface MemeShareLogRepository extends JpaRepository<MemeShareLog, Long>, MemeShareLogCustomRepository {
}
