package com.attendai.core.person.dto;

import com.attendai.core.person.entity.Gender;
import com.attendai.core.person.entity.IdentityDocType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePersonRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Middle name must not exceed 100 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    private Gender gender;

    @Past(message = "Date of birth must be a date in the past")
    private LocalDate dateOfBirth;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State/province must not exceed 100 characters")
    private String stateOrProvince;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-character ISO 3166-1 alpha-2 code")
    private String country;

    private IdentityDocType identityDocType;

    @Size(max = 100, message = "Identity document number must not exceed 100 characters")
    private String identityDocNumber;

    private Long profilePhotoFileId;
}
