package com.attendai.school.section.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * School section entity.
 *
 * <p>A section is a specific division of a class for a given academic year —
 * e.g. "Grade 5 – Section A". Sections are academic-year scoped, so a new set
 * is created each year to preserve historical enrollment and attendance data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_sections")
public class SchoolSection extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "class_id", nullable = false, updatable = false)
    private Long classId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    /** Section label — e.g. "A", "B", "Alpha". Unique per class+year. */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SectionStatus status = SectionStatus.ACTIVE;
}
