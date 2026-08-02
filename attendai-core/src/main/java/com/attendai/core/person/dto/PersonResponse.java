package com.attendai.core.person.dto;

import com.attendai.core.person.entity.Gender;
import com.attendai.core.person.entity.IdentityDocType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full person response DTO.
 *
 * {@code fullName} is a computed field assembled by the mapper.
 * PII fields (email, phone, identityDocNumber) are included — access is
 * controlled by the {@code CORE_PERSON_READ} permission.
 */
@Getter
@Builder
public class PersonResponse {
    private final Long id;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    /** Computed: firstName [middleName] lastName. Assembled by PersonMapper. */
    private final String fullName;
    private final Gender gender;
    private final LocalDate dateOfBirth;
    private final String email;
    private final String phone;
    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String stateOrProvince;
    private final String postalCode;
    private final String country;
    private final IdentityDocType identityDocType;
    private final String identityDocNumber;
    private final Long profilePhotoFileId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
