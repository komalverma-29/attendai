package com.attendai.school.teacher.entity;

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
 * School teacher entity.
 *
 * <p>Links a Core {@code Person} (identity) to a school. A Core {@code User}
 * is optional — teachers may or may not have platform login access.
 * When a user is linked, the {@code SCHOOL_TEACHER} role is automatically assigned.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_teachers")
public class SchoolTeacher extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    /** Optional. If set, must reference an ACTIVE user whose personId matches. */
    @Column(name = "user_id")
    private Long userId;

    /** School-internal unique code. Optional. Max 50 chars. */
    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "qualification", length = 255)
    private String qualification;

    @Column(name = "department", length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TeacherStatus status = TeacherStatus.ACTIVE;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "joining_date")
    private LocalDate joiningDate;
}
