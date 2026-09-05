package com.attendai.school.dailyattendance.entity;

import com.attendai.core.common.entity.BaseEntity;
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
import java.time.LocalTime;

/**
 * Daily attendance record for a single student on a single date.
 *
 * <p>One record per student per date — enforced by {@code uq_daily_att_student_date}.
 * Records are updated in-place (override workflow); there is no soft-delete.
 * Extends {@link BaseEntity} directly.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_daily_attendance")
public class DailyAttendanceRecord extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "attendance_date", nullable = false, updatable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DailyAttendanceStatus status;

    /** Nullable — present only when the student actually arrived (PRESENT / LATE). */
    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    /** FK to core attendance_events.id — nullable for ABSENT / ON_LEAVE records. */
    @Column(name = "core_event_id")
    private Long coreEventId;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /** userId who manually set or overrode this record; null when set by the system job. */
    @Column(name = "marked_by_id")
    private Long markedById;
}
