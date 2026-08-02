package com.attendai.core.person.dto;

import com.attendai.core.person.entity.Gender;
import com.attendai.core.person.entity.IdentityDocType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Update request for a person. All fields are optional.
 * Only non-null fields are applied to the existing record.
 */
@Getter
@Setter
public class UpdatePersonRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Middle name must not exceed 100 characters")
    private String middleName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    private Gender gender;

    @Past(message = "Date of birth must be a date in the past")
    private LocalDate dateOfBirth;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String stateOrProvince;

    @Size(max = 20)
    private String postalCode;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-character ISO 3166-1 alpha-2 code")
    private String country;

    private IdentityDocType identityDocType;

    @Size(max = 100)
    private String identityDocNumber;

    private Long profilePhotoFileId;
}
