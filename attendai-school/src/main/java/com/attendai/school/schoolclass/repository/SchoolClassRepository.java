package com.attendai.school.schoolclass.repository;

import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    /** All classes for a school ordered by gradeOrder ASC, optionally filtered by status. */
    @Query("""
            SELECT c FROM SchoolClass c
            WHERE c.schoolId = :schoolId
              AND (:status IS NULL OR c.status = :status)
            ORDER BY c.gradeOrder ASC
            """)
    List<SchoolClass> findBySchoolIdAndOptionalStatus(
            @Param("schoolId") Long        schoolId,
            @Param("status")   ClassStatus status);
}
