package com.attendai.school.academicyear.repository;

import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.entity.SchoolAcademicYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolAcademicYearRepository extends JpaRepository<SchoolAcademicYear, Long> {

    /** Returns the single ACTIVE year for a school if one exists. */
    Optional<SchoolAcademicYear> findBySchoolIdAndStatus(Long schoolId, AcademicYearStatus status);

    /** Checks whether a name is already used within the school (for a non-deleted year). */
    boolean existsBySchoolIdAndName(Long schoolId, String name);

    /** Paginated listing, optionally filtered by status. */
    @Query("""
            SELECT y FROM SchoolAcademicYear y
            WHERE y.schoolId = :schoolId
              AND (:status IS NULL OR y.status = :status)
            ORDER BY y.startDate DESC
            """)
    Page<SchoolAcademicYear> findBySchoolIdAndOptionalStatus(
            @Param("schoolId") Long               schoolId,
            @Param("status")   AcademicYearStatus status,
            Pageable pageable);

    /**
     * Detects date-range overlap with existing non-CANCELLED years.
     * Excludes the year with {@code excludeId} so that update operations
     * do not conflict with themselves.
     */
    @Query("""
            SELECT y FROM SchoolAcademicYear y
            WHERE y.schoolId   = :schoolId
              AND y.id        <> :excludeId
              AND y.status    <> 'CANCELLED'
              AND y.startDate <= :endDate
              AND y.endDate   >= :startDate
            """)
    List<SchoolAcademicYear> findOverlapping(
            @Param("schoolId")  Long      schoolId,
            @Param("excludeId") Long      excludeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);
}
