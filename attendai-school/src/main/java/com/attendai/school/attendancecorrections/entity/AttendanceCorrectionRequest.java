package com.attendai.school.attendancecorrections.entity;

import com.attendai.core.common.entity.BaseEntity;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
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
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "school_attendance_corrections")
public class AttendanceCorrectionRequest extends BaseEntity {

    @Column(name = "school_id",            nullable = false, updatable = false) private Long schoolId;
    @Column(name = "academic_year_id",     nullable = false, updatable = false) private Long academicYearId;
    @Column(name = "student_id",           nullable = false, updatable = false) private Long studentId;
    @Column(name = "attendance_record_id", nullable = false, updatable = false) private Long attendanceRecordId;
    @Column(name = "attendance_date",      nullable = false, updatable = false) private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_status",  nullable = false, length = 20, updatable = false)
    private DailyAttendanceStatus originalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false, length = 20, updatable = false)
    private DailyAttendanceStatus requestedStatus;

    @Column(name = "reason",           nullable = false, length = 1000) private String reason;
    @Column(name = "evidence_file_id")                                  private Long   evidenceFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CorrectionStatus status = CorrectionStatus.PENDING;

    @Column(name = "requested_by_id", nullable = false) private Long          requestedById;
    @Column(name = "reviewed_by_id")                    private Long          reviewedById;
    @Column(name = "reviewed_at")                       private LocalDateTime reviewedAt;
    @Column(name = "rejection_reason", length = 1000)   private String        rejectionReason;
}
