package com.attendai.school.school.repository;

import com.attendai.school.school.entity.School;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link School} entities.
 */
@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByCode(String code);

    /** Used by scheduled jobs to iterate all active schools. */
    List<School> findAllByStatus(SchoolStatus status);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    @Query("""
            SELECT s FROM School s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:type   IS NULL OR s.type   = :type)
              AND (:search IS NULL
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.name ASC
            """)
    Page<School> findByFilters(
            @Param("status") SchoolStatus status,
            @Param("type")   SchoolType   type,
            @Param("search") String       search,
            Pageable pageable);
}
