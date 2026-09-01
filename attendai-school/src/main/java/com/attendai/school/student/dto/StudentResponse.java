package com.attendai.school.student.dto;

import com.attendai.school.student.entity.StudentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StudentResponse {
    private final Long          id;
    private final Long          schoolId;
    private final Long          personId;
    private final Long          userId;
    private final String        admissionNumber;
    private final StudentStatus status;
    private final String        bloodGroup;
    private final String        guardianName;
    private final String        guardianPhone;
    private final String        guardianEmail;
    private final LocalDate     enrollmentDate;
    private final String        notes;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
