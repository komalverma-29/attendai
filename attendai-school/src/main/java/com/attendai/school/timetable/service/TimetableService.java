package com.attendai.school.timetable.service;

import com.attendai.school.timetable.dto.CreateTimeSlotRequest;
import com.attendai.school.timetable.dto.CreateTimetableEntryRequest;
import com.attendai.school.timetable.dto.SectionTimetableResponse;
import com.attendai.school.timetable.dto.SubjectPeriodInfo;
import com.attendai.school.timetable.dto.TimeSlotResponse;
import com.attendai.school.timetable.dto.TimetableEntryResponse;
import com.attendai.school.timetable.dto.UpdateTimetableEntryRequest;

import java.time.DayOfWeek;
import java.util.List;

public interface TimetableService {

    // -------------------------------------------------------------------------
    // Time slot management
    // -------------------------------------------------------------------------

    TimeSlotResponse createTimeSlot(Long schoolId, CreateTimeSlotRequest request);

    List<TimeSlotResponse> listTimeSlots(Long schoolId);

    void deleteTimeSlot(Long schoolId, Long timeSlotId);

    // -------------------------------------------------------------------------
    // Timetable entry management
    // -------------------------------------------------------------------------

    TimetableEntryResponse createEntry(Long schoolId, Long academicYearId,
                                        CreateTimetableEntryRequest request);

    TimetableEntryResponse updateEntry(Long schoolId, Long entryId,
                                        UpdateTimetableEntryRequest request);

    void deleteEntry(Long schoolId, Long entryId);

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------

    SectionTimetableResponse getSectionTimetable(Long schoolId, Long sectionId,
                                                  Long academicYearId);

    List<TimetableEntryResponse> getTeacherTimetable(Long schoolId, Long teacherId,
                                                      Long academicYearId);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-daily-attendance
    // -------------------------------------------------------------------------

    /**
     * Returns the list of subjects (and their assignment/slot details) that
     * have at least one period for a section on a given day of the week.
     * Called by {@code school-daily-attendance} on every attendance event cycle.
     */
    List<SubjectPeriodInfo> getSubjectsForSectionOnDay(Long sectionId, Long academicYearId,
                                                        DayOfWeek day);

    /** Returns true if any timetable entries exist for a section in an academic year. */
    boolean hasTimetableEntry(Long sectionId, Long academicYearId);
}
