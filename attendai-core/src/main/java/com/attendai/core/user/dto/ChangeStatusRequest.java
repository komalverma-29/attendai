package com.attendai.core.user.dto;

import com.attendai.core.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
