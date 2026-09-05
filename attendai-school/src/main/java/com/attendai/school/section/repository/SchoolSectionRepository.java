package com.attendai.school.section.repository;

import com.attendai.school.section.entity.SchoolSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolSectionRepository extends JpaRepository<SchoolSection, Long> {

    /** Sections for a given class + academic year, for listing. */
    List<SchoolSection> findByClassIdAndAcademicYearId(Long classId, Long academicYearId);

    /** All sections for a school in an academic year — used by dashboard section-wise summary. */
    List<SchoolSection> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);

    /** Used to enforce BR-SEC-01: name unique within class+year. */
    boolean existsByClassIdAndAcademicYearIdAndName(Long classId, Long academicYearId,
                                                     String name);

    /** Counts non-deleted sections for a class + year (used by dependency guard). */
    long countByClassIdAndAcademicYearId(Long classId, Long academicYearId);
}
