package com.attendai.school.teacherassignment.entity;

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
 * Teacher-subject-section assignment entity.
 *
 * <p>Defines which teacher teaches which subject in which section for a given
 * academic year. One assignment per subject per section per year is enforced
 * by the unique constraint {@code uq_teacher_assignments(section_id, subject_id, academic_year_id)}.
 *
 * <p>One assignment per section per year may be flagged as the class teacher
 * ({@code isClassTeacher = true}). Only one class teacher is permitted per
 * section per academic year — enforced at the service layer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_teacher_assignments")
public class TeacherAssignment extends SoftDeletableEntity {

    @Column(name = "school_id",        nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    @Column(name = "section_id",       nullable = false, updatable = false)
    private Long sectionId;

    @Column(name = "subject_id",       nullable = false, updatable = false)
    private Long subjectId;

    @Column(name = "teacher_id",       nullable = false)
    private Long teacherId;

    /** True if this teacher is the class teacher for the section this year. */
    @Column(name = "is_class_teacher", nullable = false)
    @Builder.Default
    private boolean isClassTeacher = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",           nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "notes", length = 500)
    private String notes;
}
