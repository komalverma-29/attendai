package com.attendai.school.leave.repository;

import com.attendai.school.leave.entity.LeaveApplication;
import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    /**
     * BR-LEAVE-04: Check for overlapping PENDING or APPROVED leave for same student.
     * Overlap condition: !(newEnd < existingStart || newStart > existingEnd)
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM LeaveApplication l
            WHERE l.studentId       = :studentId
              AND l.status         IN ('PENDING','APPROVED')
              AND l.startDate      <= :endDate
              AND l.endDate        >= :startDate
            """)
    boolean existsOverlappingStudentLeave(
            @Param("studentId")  Long      studentId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate);

    /**
     * BR-LEAVE-04 variant for teachers.
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM LeaveApplication l
            WHERE l.teacherId       = :teacherId
              AND l.status         IN ('PENDING','APPROVED')
              AND l.startDate      <= :endDate
              AND l.endDate        >= :startDate
            """)
    boolean existsOverlappingTeacherLeave(
            @Param("teacherId")  Long      teacherId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate);

    /** Paginated listing with optional filters. */
    @Query("""
            SELECT l FROM LeaveApplication l
            WHERE l.schoolId        = :schoolId
              AND (:studentId      IS NULL OR l.studentId      = :studentId)
              AND (:teacherId      IS NULL OR l.teacherId      = :teacherId)
              AND (:status         IS NULL OR l.status         = :status)
              AND (:leaveType      IS NULL OR l.leaveType      = :leaveType)
              AND (:fromDate       IS NULL OR l.startDate      >= :fromDate)
              AND (:toDate         IS NULL OR l.endDate        <= :toDate)
            ORDER BY l.createdAt DESC
            """)
    Page<LeaveApplication> findByFilters(
            @Param("schoolId")  Long               schoolId,
            @Param("studentId") Long               studentId,
            @Param("teacherId") Long               teacherId,
            @Param("status")    LeaveStatus        status,
            @Param("leaveType") LeaveType          leaveType,
            @Param("fromDate")  LocalDate          fromDate,
            @Param("toDate")    LocalDate          toDate,
            Pageable pageable);
}
