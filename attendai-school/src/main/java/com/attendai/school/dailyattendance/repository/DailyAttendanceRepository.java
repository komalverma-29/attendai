package com.attendai.school.dailyattendance.repository;

import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendanceRecord, Long> {

    /** BR-DA-03: idempotency check — one record per student per date. */
    boolean existsByStudentIdAndAttendanceDate(Long studentId, LocalDate date);

    Optional<DailyAttendanceRecord> findByStudentIdAndAttendanceDate(
            Long studentId, LocalDate date);

    /** All records for a section on a specific date. Used by FR-DA-03. */
    List<DailyAttendanceRecord> findBySectionIdAndAttendanceDate(
            Long sectionId, LocalDate date);

    /** All records for a student within a date range. Used by FR-DA-04. */
    List<DailyAttendanceRecord> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long studentId, LocalDate fromDate, LocalDate toDate);

    /** All records for a section within a date range. Used for summary. */
    List<DailyAttendanceRecord> findBySectionIdAndAttendanceDateBetween(
            Long sectionId, LocalDate fromDate, LocalDate toDate);

    /** Count by status for a section on a date. Used by FR-DA-05. */
    @Query("""
            SELECT r.status, COUNT(r) FROM DailyAttendanceRecord r
            WHERE r.sectionId = :sectionId AND r.attendanceDate = :date
            GROUP BY r.status
            """)
    List<Object[]> countByStatusForSectionDate(
            @Param("sectionId") Long sectionId,
            @Param("date")      LocalDate date);

    /** Used by leave approval to find existing records in a date range. */
    List<DailyAttendanceRecord> findByStudentIdAndAttendanceDateIn(
            Long studentId, List<LocalDate> dates);

    /** Count records for school+date — used by scheduler to check if job already ran. */
    long countBySchoolIdAndAttendanceDate(Long schoolId, LocalDate date);
}
