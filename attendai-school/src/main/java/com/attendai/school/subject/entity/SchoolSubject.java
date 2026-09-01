package com.attendai.school.subject.entity;

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
 * School subject entity.
 *
 * <p>A subject represents a curriculum area taught to students (e.g. Mathematics, English).
 * Subjects are school-scoped and persist across academic years.
 * They are referenced by teacher assignments and timetables.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_subjects")
public class SchoolSubject extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    /** Display name — unique within a school. */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Short uppercase code — unique within a school (e.g. "MATH", "ENG"). */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SubjectType type;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubjectStatus status = SubjectStatus.ACTIVE;
}
