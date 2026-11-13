package com.frandm.notificationcenter.notification.repository;

import com.frandm.notificationcenter.notification.model.Notification;
import com.frandm.notificationcenter.notification.model.NotificationLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findFirstByDedupKeyAndReadFalseOrderByCreatedAtDesc(String dedupKey);

    long countByReadFalse();

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.read = false")
    int markAllAsRead();

    @Query("SELECT DISTINCT n.source FROM Notification n ORDER BY n.source ASC")
    List<String> findDistinctSources();

    @Query("""
        SELECT n FROM Notification n
        WHERE (:source IS NULL OR n.source = :source)
          AND (:isRead IS NULL OR n.read = :isRead)
          AND (:level IS NULL OR n.level = :level)
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findFiltered(
            @Param("source") String source,
            @Param("isRead") Boolean isRead,
            @Param("level") NotificationLevel level,
            Pageable pageable
    );
}
