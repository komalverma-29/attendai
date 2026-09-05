package com.attendai.school.dailyattendance.scheduler;

import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs once per day at the school's configured mark-absent time (default 11:00).
 * For every active enrolled student with no attendance record today, creates an ABSENT record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkAbsentJob {

    private final DailyAttendanceService dailyAttendanceService;
    private final SchoolRepository       schoolRepository;

    /** Runs daily at 11:00 AM (overridable via school-settings per school). */
    @Scheduled(cron = "${school.attendance.mark-absent-cron:0 0 11 * * MON-FRI}")
    public void run() {
        log.info("MarkAbsentJob: starting");
        schoolRepository.findAllByStatus(SchoolStatus.ACTIVE).forEach(school -> {
            try {
                dailyAttendanceService.runMarkAbsentJob(school.getId());
            } catch (Exception e) {
                log.error("MarkAbsentJob failed for school {}: {}",
                        school.getId(), e.getMessage(), e);
            }
        });
        log.info("MarkAbsentJob: complete");
    }
}
