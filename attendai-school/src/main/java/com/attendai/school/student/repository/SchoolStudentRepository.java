package com.attendai.school.student.repository;

import com.attendai.school.student.entity.SchoolStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolStudentRepository extends JpaRepository<SchoolStudent, Long> {

    boolean existsByPersonIdAndSchoolId(Long personId, Long schoolId);

    boolean existsBySchoolIdAndAdmissionNumber(Long schoolId, String admissionNumber);

    Optional<SchoolStudent> findByUserId(Long userId);

    Optional<SchoolStudent> findByPersonIdAndSchoolId(Long personId, Long schoolId);

    @Query("""
            SELECT s FROM SchoolStudent s
            WHERE s.schoolId = :schoolId
              AND (:search IS NULL
                   OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.enrollmentDate DESC
            """)
    Page<SchoolStudent> findBySchoolIdAndSearch(
            @Param("schoolId") Long   schoolId,
            @Param("search")   String search,
            Pageable pageable);
}
