package com.attendai.school.school.dto;

import com.attendai.school.school.entity.SchoolType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSchoolRequest {

    @NotBlank(message = "School name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    /**
     * Optional short code. If omitted, one is auto-generated.
     * Must be uppercase alphanumeric, 4–10 characters.
     */
    @Size(min = 2, max = 10, message = "Code must be between 2 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "Code must be uppercase letters and digits only")
    private String code;

    @NotNull(message = "School type is required")
    private SchoolType type;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State/Province is required")
    @Size(max = 100)
    private String stateOrProvince;

    @Size(max = 20)
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-character ISO 3166-1 alpha-2 code")
    private String country;

    @Size(max = 30)
    private String phone;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String website;
}
