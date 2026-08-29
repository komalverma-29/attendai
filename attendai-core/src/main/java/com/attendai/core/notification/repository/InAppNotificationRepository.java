package com.attendai.core.notification.repository;

import com.attendai.core.notification.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    /** Returns the user's inbox, optionally filtered by read status. */
    @Query("""
            SELECT n FROM InAppNotification n
            WHERE n.recipientUserId = :userId
              AND (:read IS NULL OR n.isRead = :read)
            ORDER BY n.createdAt DESC
            """)
    Page<InAppNotification> findInbox(
            @Param("userId") Long    userId,
            @Param("read")   Boolean read,
            Pageable pageable);

    /** Marks all unread in-app notifications for a user as read. */
    @Modifying
    @Query("""
            UPDATE InAppNotification n
               SET n.isRead    = true,
                   n.readAt    = CURRENT_TIMESTAMP,
                   n.updatedAt = CURRENT_TIMESTAMP
             WHERE n.recipientUserId = :userId
               AND n.isRead = false
            """)
    int markAllReadByUserId(@Param("userId") Long userId);
}
