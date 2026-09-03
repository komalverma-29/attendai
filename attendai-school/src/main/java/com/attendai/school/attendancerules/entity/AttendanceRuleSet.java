package com.attendai.school.attendancerules.entity;

import com.attendai.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * School-level attendance rule set for a specific academic year.
 *
 * <p>One rule set per (school, academic year) — enforced by the unique constraint
 * {@code uq_rule_sets(school_id, academic_year_id)}.
 *
 * <p>Not soft-deleted: rule sets are updated in-place or hard-deleted.
 * Extends {@link BaseEntity} directly.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_attendance_rule_sets")
public class AttendanceRuleSet extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    /** Time after which a student is marked LATE rather than PRESENT. Default 09:00. */
    @Column(name = "late_threshold_time", nullable = false)
    private LocalTime lateThresholdTime;

    /** Minimum attendance percentage required. Default 75.00. */
    @Column(name = "min_attendance_percentage", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal minAttendancePercentage;

    /** Number of consecutive absences that triggers a guardian notification. Default 3. */
    @Column(name = "consecutive_absence_alert", nullable = false)
    private int consecutiveAbsenceAlert;
}
