package com.attendai.core.audit.repository;

import com.attendai.core.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository for audit log records.
 * Provides read-only queries; writes go through {@link com.attendai.core.audit.service.AuditServiceImpl}.
 * No update or delete methods are defined — audit records are immutable.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:actionCode IS NULL OR a.actionCode = :actionCode)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (:module IS NULL OR a.module = :module)
              AND (:fromDate IS NULL OR a.occurredAt >= :fromDate)
              AND (:toDate IS NULL OR a.occurredAt <= :toDate)
            ORDER BY a.occurredAt DESC
            """)
    Page<AuditLog> findByFilters(
            @Param("actorUserId") Long actorUserId,
            @Param("actionCode") String actionCode,
            @Param("resourceType") String resourceType,
            @Param("module") String module,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
