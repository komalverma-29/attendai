package com.attendai.school.subject.repository;

import com.attendai.school.subject.entity.SchoolSubject;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolSubjectRepository extends JpaRepository<SchoolSubject, Long> {

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    boolean existsBySchoolIdAndCode(Long schoolId, String code);

    /**
     * Lists subjects for a school with optional type and status filters.
     * Ordered by name ascending.
     */
    @Query("""
            SELECT s FROM SchoolSubject s
            WHERE s.schoolId = :schoolId
              AND (:type   IS NULL OR s.type   = :type)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.name ASC
            """)
    List<SchoolSubject> findBySchoolIdAndFilters(
            @Param("schoolId") Long          schoolId,
            @Param("type")     SubjectType   type,
            @Param("status")   SubjectStatus status);

    /**
     * Lists subjects linked to a specific class via the class_subjects join table.
     */
    @Query("""
            SELECT s FROM SchoolSubject s
            WHERE s.id IN (
                SELECT cs.subjectId FROM ClassSubject cs
                WHERE cs.classId = :classId
            )
            ORDER BY s.name ASC
            """)
    List<SchoolSubject> findByClassId(@Param("classId") Long classId);
}
