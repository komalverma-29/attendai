package com.attendai.school.schoolclass.entity;

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
 * School class (grade) entity.
 *
 * <p>Represents a grade level within a school (e.g. "Grade 5", "Standard 10").
 * Classes persist across academic years. Sections are created within a class.
 *
 * <p>Package is {@code com.attendai.school.schoolclass} because {@code class}
 * is a reserved keyword in Java.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_classes")
public class SchoolClass extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    /** Grade name — unique within the school. E.g. "Grade 5". */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Optional alternate display name. E.g. "5th Standard". */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * Numeric ordering used when listing classes.
     * Grade 1 = 1, Grade 2 = 2, etc.
     */
    @Column(name = "grade_order", nullable = false)
    private int gradeOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ClassStatus status = ClassStatus.ACTIVE;
}
