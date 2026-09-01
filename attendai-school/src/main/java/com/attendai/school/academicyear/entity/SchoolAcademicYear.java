package com.attendai.school.academicyear.entity;

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

import java.time.LocalDate;

/**
 * School academic year entity.
 *
 * <p>An academic year defines the time boundary within which all school operations
 * (sections, timetables, attendance) are scoped.
 *
 * <p>Only one year may be {@link AcademicYearStatus#ACTIVE} per school at any time.
 * This invariant is enforced in the service layer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_academic_years")
public class SchoolAcademicYear extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    /** Display name, e.g. "2025-2026". Unique within a school (enforced by DB unique index). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AcademicYearStatus status = AcademicYearStatus.UPCOMING;

    /** Optional free-text description. */
    @Column(name = "description", length = 500)
    private String description;
}
