package com.attendai.school.timetable.repository;

import com.attendai.school.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    /**
     * BR-TT-01: one entry per section×timeslot×day×year.
     * Checked before save to give a meaningful error message before the DB constraint fires.
     */
    boolean existsBySectionIdAndTimeSlotIdAndDayOfWeekAndAcademicYearId(
            Long sectionId, Long timeSlotId, DayOfWeek dayOfWeek, Long academicYearId);

    /**
     * BR-TT-02: teacher cannot be in two sections at the same slot on the same day.
     * Checked before save.
     */
    boolean existsByAssignmentIdAndTimeSlotIdAndDayOfWeek(
            Long assignmentId, Long timeSlotId, DayOfWeek dayOfWeek);

    /** All entries for a section in an academic year — used to build section timetable. */
    List<TimetableEntry> findBySectionIdAndAcademicYearId(Long sectionId, Long academicYearId);

    /** All entries for a specific day — used by internal API for daily attendance. */
    @Query("""
            SELECT e FROM TimetableEntry e
            WHERE e.sectionId       = :sectionId
              AND e.academicYearId  = :academicYearId
              AND e.dayOfWeek       = :dayOfWeek
            ORDER BY e.timeSlotId   ASC
            """)
    List<TimetableEntry> findBySectionIdAndAcademicYearIdAndDayOfWeek(
            @Param("sectionId")      Long      sectionId,
            @Param("academicYearId") Long      academicYearId,
            @Param("dayOfWeek")      DayOfWeek dayOfWeek);

    /** All entries for a given assignment — used for assignment-deletion guard. */
    List<TimetableEntry> findByAssignmentId(Long assignmentId);

    /** Count entries for a section in a year — used by hasTimetableEntry internal API. */
    long countBySectionIdAndAcademicYearId(Long sectionId, Long academicYearId);

    /** All entries for a teacher (via assignment) in a year — used for teacher timetable. */
    @Query("""
            SELECT e FROM TimetableEntry e
            WHERE e.academicYearId = :academicYearId
              AND e.assignmentId   IN (
                  SELECT a.id FROM TeacherAssignment a
                  WHERE a.teacherId = :teacherId
                    AND a.academicYearId = :academicYearId
              )
            ORDER BY e.dayOfWeek ASC, e.timeSlotId ASC
            """)
    List<TimetableEntry> findByTeacherIdAndAcademicYearId(
            @Param("teacherId")      Long teacherId,
            @Param("academicYearId") Long academicYearId);
}
