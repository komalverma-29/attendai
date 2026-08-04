package com.attendai.core.attendance.repository;

import com.attendai.core.attendance.entity.AttendanceEvent;
import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    // -------------------------------------------------------------------------
    // Deduplication query — must use the composite index (person_id, station_id,
    // direction, event_time) for fast O(log n) lookups.
    // -------------------------------------------------------------------------

    /**
     * Checks whether a recent PENDING or PROCESSED event exists for the same
     * person, station, and direction within the deduplication window.
     * Returns the most recent one if found.
     */
    @Query("""
            SELECT e FROM AttendanceEvent e
            WHERE e.personId   = :personId
              AND e.stationId  = :stationId
              AND e.direction  = :direction
              AND e.status    IN ('PENDING', 'PROCESSED')
              AND e.eventTime >= :windowStart
            ORDER BY e.eventTime DESC
            LIMIT 1
            """)
    List<AttendanceEvent> findRecentForDeduplication(
            @Param("personId")    Long           personId,
            @Param("stationId")   Long           stationId,
            @Param("direction")   EventDirection direction,
            @Param("windowStart") LocalDateTime  windowStart);

    // -------------------------------------------------------------------------
    // General filter query
    // -------------------------------------------------------------------------

    @Query("""
            SELECT e FROM AttendanceEvent e
            WHERE (:personId  IS NULL OR e.personId  = :personId)
              AND (:stationId IS NULL OR e.stationId = :stationId)
              AND (:status    IS NULL OR e.status    = :status)
              AND (:source    IS NULL OR e.source    = :source)
              AND (:direction IS NULL OR e.direction = :direction)
              AND (:fromDate  IS NULL OR e.eventTime >= :fromDate)
              AND (:toDate    IS NULL OR e.eventTime <= :toDate)
            ORDER BY e.eventTime DESC
            """)
    Page<AttendanceEvent> findByFilters(
            @Param("personId")  Long                   personId,
            @Param("stationId") Long                   stationId,
            @Param("status")    AttendanceEventStatus  status,
            @Param("source")    EventSource            source,
            @Param("direction") EventDirection         direction,
            @Param("fromDate")  LocalDateTime          fromDate,
            @Param("toDate")    LocalDateTime          toDate,
            Pageable pageable);

    // -------------------------------------------------------------------------
    // Pending events polling — used by business modules
    // -------------------------------------------------------------------------

    @Query("""
            SELECT e FROM AttendanceEvent e
            WHERE e.status = 'PENDING'
            ORDER BY e.eventTime ASC
            """)
    Page<AttendanceEvent> findPending(Pageable pageable);

    // -------------------------------------------------------------------------
    // Internal API for business modules
    // -------------------------------------------------------------------------

    /** Find PENDING events for a specific person on a specific date (UTC day). */
    @Query("""
            SELECT e FROM AttendanceEvent e
            WHERE e.personId   = :personId
              AND e.eventTime >= :dayStart
              AND e.eventTime <  :dayEnd
              AND e.status     = 'PENDING'
            ORDER BY e.eventTime ASC
            """)
    List<AttendanceEvent> findPendingByPersonAndDate(
            @Param("personId") Long          personId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd);

    /** Find all events for a person within a datetime range (any status). */
    @Query("""
            SELECT e FROM AttendanceEvent e
            WHERE e.personId   = :personId
              AND e.eventTime >= :from
              AND e.eventTime <= :to
            ORDER BY e.eventTime ASC
            """)
    List<AttendanceEvent> findByPersonAndDateRange(
            @Param("personId") Long          personId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    /** Count events for a person on a specific date (used for attendance calculations). */
    @Query("""
            SELECT COUNT(e) FROM AttendanceEvent e
            WHERE e.personId   = :personId
              AND e.eventTime >= :dayStart
              AND e.eventTime <  :dayEnd
              AND e.status    IN ('PENDING', 'PROCESSED')
            """)
    int countByPersonAndDate(
            @Param("personId") Long          personId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd);
}
