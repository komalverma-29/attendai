package com.attendai.school.dailyattendance.scheduler;

import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls core-attendance for PENDING events every 5 minutes and processes them
 * into DailyAttendanceRecord rows.
 *
 * <p>Runs for all ACTIVE schools independently.
 * Each school's processing is independent and failure-isolated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceProcessingJob {

    private final DailyAttendanceService dailyAttendanceService;
    private final SchoolRepository       schoolRepository;

    /** Every 5 minutes during school hours. Configurable via school-settings. */
    @Scheduled(fixedDelayString = "${school.attendance.processing-interval-ms:300000}")
    public void run() {
        log.debug("AttendanceProcessingJob: starting cycle");
        schoolRepository.findAllByStatus(SchoolStatus.ACTIVE).forEach(school -> {
            try {
                dailyAttendanceService.processSchoolAttendanceEvents(school.getId());
            } catch (Exception e) {
                log.error("AttendanceProcessingJob failed for school {}: {}",
                        school.getId(), e.getMessage(), e);
            }
        });
        log.debug("AttendanceProcessingJob: cycle complete");
    }
}
