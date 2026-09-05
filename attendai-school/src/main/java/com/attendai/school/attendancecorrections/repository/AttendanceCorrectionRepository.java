package com.attendai.school.attendancecorrections.repository;

import com.attendai.school.attendancecorrections.entity.AttendanceCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AttendanceCorrectionRepository
        extends JpaRepository<AttendanceCorrectionRequest, Long> {

    /** BR-CORR-04: one PENDING per student per date. */
    boolean existsByStudentIdAndAttendanceDateAndStatus(
            Long studentId, LocalDate date, CorrectionStatus status);

    @Query("""
            SELECT c FROM AttendanceCorrectionRequest c
            WHERE c.schoolId        = :schoolId
              AND (:studentId  IS NULL OR c.studentId      = :studentId)
              AND (:status     IS NULL OR c.status         = :status)
              AND (:fromDate   IS NULL OR c.attendanceDate >= :fromDate)
              AND (:toDate     IS NULL OR c.attendanceDate <= :toDate)
            ORDER BY c.attendanceDate DESC
            """)
    Page<AttendanceCorrectionRequest> findByFilters(
            @Param("schoolId")  Long             schoolId,
            @Param("studentId") Long             studentId,
            @Param("status")    CorrectionStatus status,
            @Param("fromDate")  LocalDate        fromDate,
            @Param("toDate")    LocalDate        toDate,
            Pageable pageable);
}
