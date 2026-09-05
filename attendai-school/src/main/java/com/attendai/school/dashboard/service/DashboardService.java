package com.attendai.school.dashboard.service;

import com.attendai.school.dashboard.dto.AttendanceTrendResponse;
import com.attendai.school.dashboard.dto.LowAttendanceAlertResponse;
import com.attendai.school.dashboard.dto.SchoolOverviewResponse;
import com.attendai.school.dashboard.dto.SectionDailySummaryResponse;

import java.util.List;

public interface DashboardService {
    SchoolOverviewResponse getSchoolOverview(Long schoolId);
    List<SectionDailySummaryResponse> getSectionsSummaryToday(Long schoolId);
    AttendanceTrendResponse getAttendanceTrend(Long schoolId, Long sectionId);
    List<LowAttendanceAlertResponse> getLowAttendanceAlerts(Long schoolId, Long sectionId);
    SectionDailySummaryResponse getSectionDashboard(Long schoolId, Long sectionId);
}
