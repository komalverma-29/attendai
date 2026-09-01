package com.attendai.school.academicyear.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateAcademicYearRequest {

    /** Name update is allowed for UPCOMING and ACTIVE years. */
    @Size(max = 100)
    private String name;

    /** Date-range updates are only permitted for UPCOMING years (BR-AY-04). */
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500)
    private String description;
}
