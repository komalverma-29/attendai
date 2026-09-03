package com.attendai.school.academiccalendar.entity;

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

/**
 * A single calendar exception entry for a school's academic year.
 *
 * <p>Only exceptions to the default schedule are stored here:
 * <ul>
 *   <li>{@link CalendarEntryType#HOLIDAY} — a working day made non-working</li>
 *   <li>{@link CalendarEntryType#WORKING_DAY} — a weekend/holiday made working</li>
 * </ul>
 *
 * <p>Regular weekday working days are implicit and have NO entry.
 * The absence of an entry means: use the school's weekend configuration to decide.
 *
 * <p>Extends {@link BaseEntity} directly — NOT {@code SoftDeletableEntity}.
 * Calendar entries are <em>hard-deleted</em>. Deleting a holiday reverts the date
 * to its default status. Soft-delete would be semantically confusing since a missing
 * entry already has meaning.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_calendar_entries")
public class SchoolCalendarEntry extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private Long academicYearId;

    /** The specific date. Unique within (school, academic year). */
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private CalendarEntryType entryType;

    /** Human-readable name, e.g. "Diwali", "Republic Day". */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;
}
