package com.attendai.school.attendancereports.service;

import com.attendai.school.attendancereports.dto.AttendanceShortageResponse;
import com.attendai.school.attendancereports.dto.ConsecutiveAbsenceResponse;
import com.attendai.school.attendancereports.dto.DailyAttendanceRegisterResponse;
import com.attendai.school.attendancereports.dto.SchoolAttendanceOverviewResponse;
import com.attendai.school.attendancereports.dto.StudentAttendanceSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceReportService {
    StudentAttendanceSummaryResponse getStudentSummary(Long schoolId, Long studentId,
                                                        Long academicYearId,
                                                        LocalDate fromDate, LocalDate toDate);
    List<StudentAttendanceSummaryResponse> getSectionSummary(Long schoolId, Long sectionId,
                                                              Long academicYearId,
                                                              LocalDate fromDate, LocalDate toDate);
    List<AttendanceShortageResponse> getShortageReport(Long schoolId, Long academicYearId,
                                                        Long sectionId);
    DailyAttendanceRegisterResponse getDailyRegister(Long schoolId, Long sectionId,
                                                      Long academicYearId,
                                                      LocalDate fromDate, LocalDate toDate);
    List<ConsecutiveAbsenceResponse> getConsecutiveAbsences(Long schoolId, Long academicYearId,
                                                             Long sectionId, int minDays);
    SchoolAttendanceOverviewResponse getSchoolOverview(Long schoolId,
                                                        LocalDate fromDate, LocalDate toDate);
}
