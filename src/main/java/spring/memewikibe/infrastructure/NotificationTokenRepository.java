package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import spring.memewikibe.domain.notification.NotificationToken;

import java.util.List;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, String> {

    @Query("SELECT n.token FROM NotificationToken n")
    List<String> findAllTokens();
}
