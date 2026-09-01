package com.attendai.school.school.dto;

import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Full school response. */
@Getter
@Builder
public class SchoolResponse {
    private final Long         id;
    private final String       name;
    private final String       code;
    private final SchoolType   type;
    private final SchoolStatus status;
    private final String       description;
    private final String       addressLine1;
    private final String       addressLine2;
    private final String       city;
    private final String       stateOrProvince;
    private final String       postalCode;
    private final String       country;
    private final String       phone;
    private final String       email;
    private final String       website;
    private final Long         logoFileId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
