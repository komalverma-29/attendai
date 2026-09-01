package com.attendai.school.administrator.dto;

import com.attendai.school.administrator.entity.AdministratorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeAdministratorStatusRequest {

    @NotNull(message = "Status is required")
    private AdministratorStatus status;
}
