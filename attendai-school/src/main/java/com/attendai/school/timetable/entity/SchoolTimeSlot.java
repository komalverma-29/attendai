package com.attendai.school.timetable.entity;

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

import java.time.LocalTime;

/**
 * A named time slot (period) within a school's daily schedule.
 *
 * <p>Time slots are school-wide and reused across sections. They define
 * the temporal structure of the timetable (e.g. Period 1: 09:00–09:45).
 * Slots are ordered by {@code slotOrder} for display.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_time_slots")
public class SchoolTimeSlot extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    /** Display name unique within school — e.g. "Period 1", "Lunch". */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Display order for listing — lower values appear first. */
    @Column(name = "slot_order", nullable = false)
    private int slotOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, length = 20)
    @Builder.Default
    private TimeSlotType slotType = TimeSlotType.PERIOD;
}
