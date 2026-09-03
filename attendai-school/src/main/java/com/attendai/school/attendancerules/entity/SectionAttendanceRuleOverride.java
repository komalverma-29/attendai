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
 * Section-level override for an attendance rule set.
 *
 * <p>NULL field values mean "fall back to the school-level rule set value".
 * The effective rule for a section is computed by merging: section non-null values
 * take precedence over school-level values.
 *
 * <p>At most one override per (rule_set_id, section_id) — enforced by
 * {@code uq_section_overrides}. Not soft-deleted.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_section_attendance_overrides")
public class SectionAttendanceRuleOverride extends BaseEntity {

    @Column(name = "rule_set_id", nullable = false, updatable = false)
    private Long ruleSetId;

    @Column(name = "section_id", nullable = false, updatable = false)
    private Long sectionId;

    /** Nullable — null means use school-level value. */
    @Column(name = "late_threshold_time")
    private LocalTime lateThresholdTime;

    /** Nullable — null means use school-level value. */
    @Column(name = "min_attendance_percentage", precision = 5, scale = 2)
    private BigDecimal minAttendancePercentage;

    /**
     * Nullable override. Null means use school-level value.
     * Stored as Integer (boxed) so null is distinguishable from 0.
     */
    @Column(name = "consecutive_absence_alert")
    private Integer consecutiveAbsenceAlert;
}
