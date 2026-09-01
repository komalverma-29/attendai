package com.attendai.school.school.dto;

import com.attendai.school.school.entity.SchoolType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** All fields are optional — only non-null fields are applied. */
@Getter
@Setter
public class UpdateSchoolRequest {

    @Size(max = 255)
    private String name;

    private SchoolType type;

    @Size(max = 1000)
    private String description;

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

    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String website;

    private Long logoFileId;
}
