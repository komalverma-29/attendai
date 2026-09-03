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

import java.time.DayOfWeek;

/**
 * A single cell in the weekly timetable grid.
 *
 * <p>Assigns a teacher-assignment (subject+teacher in a section) to a specific
 * time slot on a specific day of the week for an academic year.
 *
 * <p>Two unique constraints are enforced at the DB level:
 * <ul>
 *   <li>{@code uq_timetable_slot} — one entry per section×timeslot×day×year (BR-TT-01)</li>
 *   <li>{@code uq_timetable_teacher} — one entry per assignment×timeslot×day (BR-TT-02)</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_timetable_entries")
public class TimetableEntry extends SoftDeletableEntity {

    @Column(name = "school_id",        nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    @Column(name = "section_id",       nullable = false, updatable = false)
    private Long sectionId;

    @Column(name = "time_slot_id",     nullable = false)
    private Long timeSlotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week",      nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /** FK to school_teacher_assignments. Must be ACTIVE (BR-TT-03). */
    @Column(name = "assignment_id",    nullable = false)
    private Long assignmentId;
}
