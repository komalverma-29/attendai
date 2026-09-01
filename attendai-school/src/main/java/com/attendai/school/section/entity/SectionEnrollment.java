package com.attendai.school.section.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Student–section enrollment record.
 *
 * <p>Links a student to a section for a given academic year with a roll number.
 * Enrollment is NOT soft-deleted — removal is a hard delete (the record is simply
 * deleted if the student is unenrolled). This preserves the constraint that a
 * student can only be in one section per academic year.
 *
 * <p>Does NOT extend {@code SoftDeletableEntity} or {@code BaseEntity} because the
 * SDD specifies only created_at/updated_at/created_by/updated_by audit columns with
 * no is_deleted column.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_section_enrollments")
@EntityListeners(AuditingEntityListener.class)
public class SectionEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "section_id", nullable = false, updatable = false)
    private Long sectionId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    @Column(name = "roll_number", nullable = false, length = 20)
    private String rollNumber;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDate enrolledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
