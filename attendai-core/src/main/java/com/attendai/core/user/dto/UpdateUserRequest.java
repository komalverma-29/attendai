package com.attendai.core.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Username must not exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._\\-]+$",
             message = "Username may only contain letters, digits, dots, hyphens, and underscores")
    private String username;

    private Boolean mustChangePassword;
}
