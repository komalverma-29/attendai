package com.attendai.core.station.repository;

import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    /** Lookup by API key hash — used by StationAuthenticationFilter on every request. */
    Optional<Station> findByApiKeyHash(String apiKeyHash);

    boolean existsByName(String name);

    @Query("""
            SELECT s FROM Station s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:type   IS NULL OR s.type   = :type)
            ORDER BY s.name ASC
            """)
    Page<Station> findByFilters(
            @Param("status") StationStatus status,
            @Param("type")   StationType   type,
            Pageable pageable);

    /**
     * Lightweight heartbeat update — only sets last_seen_at.
     * Called by the heartbeat endpoint for minimal DB overhead.
     */
    @Modifying
    @Query("UPDATE Station s SET s.lastSeenAt = :now WHERE s.id = :stationId")
    void updateLastSeenAt(@Param("stationId") Long stationId,
                          @Param("now")       LocalDateTime now);
}
