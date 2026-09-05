package com.attendai.school.attendancereports.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter @Builder
public class DailyAttendanceRegisterResponse {
    private final Long            sectionId;
    private final List<LocalDate> workingDates;
    private final List<StudentRegisterRow> students;

    @Getter @Builder
    public static class StudentRegisterRow {
        private final Long         studentId;
        private final String       rollNumber;
        /** Status codes per working date: P, L, A, OL, "-" (no record). */
        private final List<String> attendance;
    }
}
