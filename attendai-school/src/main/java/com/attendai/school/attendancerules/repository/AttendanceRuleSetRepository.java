package com.attendai.school.attendancerules.repository;

import com.attendai.school.attendancerules.entity.AttendanceRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRuleSetRepository extends JpaRepository<AttendanceRuleSet, Long> {

    /** BR-RULES-01: uniqueness check before insert. */
    boolean existsBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);

    /** Primary lookup for a school+year rule set. */
    Optional<AttendanceRuleSet> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);
}
