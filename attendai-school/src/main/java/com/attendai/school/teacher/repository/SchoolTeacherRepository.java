package com.attendai.school.teacher.repository;

import com.attendai.school.teacher.entity.SchoolTeacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolTeacherRepository extends JpaRepository<SchoolTeacher, Long> {

    boolean existsByPersonIdAndSchoolId(Long personId, Long schoolId);

    boolean existsBySchoolIdAndEmployeeCode(Long schoolId, String employeeCode);

    Optional<SchoolTeacher> findByUserId(Long userId);

    @Query("""
            SELECT t FROM SchoolTeacher t
            WHERE t.schoolId = :schoolId
              AND (:search IS NULL
                   OR LOWER(t.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY t.createdAt DESC
            """)
    Page<SchoolTeacher> findBySchoolIdAndSearch(
            @Param("schoolId") Long   schoolId,
            @Param("search")   String search,
            Pageable pageable);
}
