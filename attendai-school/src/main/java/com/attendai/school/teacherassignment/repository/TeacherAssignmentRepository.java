package com.attendai.school.teacherassignment.repository;

import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import com.attendai.school.teacherassignment.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    /**
     * BR-ASSIGN-01 guard: at most one active assignment per subject per section per year.
     * The DB unique constraint covers all statuses, but we check for existing (non-deleted)
     * records too.
     */
    boolean existsBySectionIdAndSubjectIdAndAcademicYearId(
            Long sectionId, Long subjectId, Long academicYearId);

    /**
     * BR-ASSIGN-05 guard: only one class teacher per section per year.
     */
    boolean existsBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(
            Long sectionId, Long academicYearId);

    /** List all assignments for a section + year. Used by FR-ASSIGN-03 and internal API. */
    List<TeacherAssignment> findBySectionIdAndAcademicYearId(Long sectionId, Long academicYearId);

    /** List all assignments for a teacher in a year. Used by FR-ASSIGN-04. */
    List<TeacherAssignment> findByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);

    /** List all assignments for a subject in a year (optional filter). */
    List<TeacherAssignment> findBySubjectIdAndAcademicYearId(Long subjectId, Long academicYearId);

    /** Flexible listing with optional filters. */
    @Query("""
            SELECT a FROM TeacherAssignment a
            WHERE a.schoolId       = :schoolId
              AND a.academicYearId = :academicYearId
              AND (:sectionId  IS NULL OR a.sectionId  = :sectionId)
              AND (:teacherId  IS NULL OR a.teacherId  = :teacherId)
              AND (:subjectId  IS NULL OR a.subjectId  = :subjectId)
            ORDER BY a.createdAt ASC
            """)
    List<TeacherAssignment> findByFilters(
            @Param("schoolId")       Long schoolId,
            @Param("academicYearId") Long academicYearId,
            @Param("sectionId")      Long sectionId,
            @Param("teacherId")      Long teacherId,
            @Param("subjectId")      Long subjectId);

    /** Used by class-teacher designation: find the current class teacher if any. */
    Optional<TeacherAssignment> findBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(
            Long sectionId, Long academicYearId);

    /** Count ACTIVE assignments for a teacher in a year (deletion guard check). */
    long countByTeacherIdAndAcademicYearIdAndStatus(Long teacherId, Long academicYearId,
                                                      AssignmentStatus status);
}
