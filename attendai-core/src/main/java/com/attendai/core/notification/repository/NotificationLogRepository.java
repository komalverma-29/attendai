package com.attendai.core.notification.repository;

import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.NotificationLog;
import com.attendai.core.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * Finds FAILED logs that are eligible for retry.
     * Excludes logs that have already reached max attempts (handled at service layer).
     */
    List<NotificationLog> findByStatusAndAttemptCountLessThan(
            NotificationStatus status, int maxAttempts);

    /**
     * Finds PENDING logs whose scheduled time has passed — ready to send.
     */
    List<NotificationLog> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status, LocalDateTime now);

    @Query("""
            SELECT l FROM NotificationLog l
            WHERE (:userId    IS NULL OR l.recipientUserId = :userId)
              AND (:typeCode  IS NULL OR l.typeCode        = :typeCode)
              AND (:channel   IS NULL OR l.channel         = :channel)
              AND (:status    IS NULL OR l.status          = :status)
              AND (:fromDate  IS NULL OR l.createdAt       >= :fromDate)
              AND (:toDate    IS NULL OR l.createdAt       <= :toDate)
            ORDER BY l.createdAt DESC
            """)
    Page<NotificationLog> findByFilters(
            @Param("userId")   Long             userId,
            @Param("typeCode") String           typeCode,
            @Param("channel")  Channel          channel,
            @Param("status")   NotificationStatus status,
            @Param("fromDate") LocalDateTime    fromDate,
            @Param("toDate")   LocalDateTime    toDate,
            Pageable pageable);
}
