package com.attendai.school.section.repository;

import com.attendai.school.section.entity.SectionEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionEnrollmentRepository extends JpaRepository<SectionEnrollment, Long> {

    /** BR-SEC-02: a student can only be in one section per academic year. */
    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    /** BR-SEC-03: roll number unique within section+year. */
    boolean existsBySectionIdAndAcademicYearIdAndRollNumber(Long sectionId, Long academicYearId,
                                                             String rollNumber);

    /** Lookup for removal and student-section queries. */
    Optional<SectionEnrollment> findBySectionIdAndStudentId(Long sectionId, Long studentId);

    /** All students enrolled in a section, ordered by roll number. */
    List<SectionEnrollment> findBySectionIdOrderByRollNumberAsc(Long sectionId);

    /** Find what section a student is in for a given academic year. */
    Optional<SectionEnrollment> findByStudentIdAndAcademicYearId(Long studentId,
                                                                   Long academicYearId);

    /** Count enrollments in a section (used for delete guard). */
    long countBySectionId(Long sectionId);

    /** Used for isStudentEnrolledInSection internal API. */
    boolean existsBySectionIdAndStudentIdAndAcademicYearId(Long sectionId, Long studentId,
                                                            Long academicYearId);
}
