package spring.memewikibe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.memewikibe.domain.notification.NotificationToken;

import java.util.List;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, String> {
    List<NotificationToken> findByIsActiveTrueAndPushEnabledTrue();
}
