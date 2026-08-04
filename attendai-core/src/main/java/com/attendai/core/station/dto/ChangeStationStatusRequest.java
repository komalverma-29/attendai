package com.attendai.core.station.dto;

import com.attendai.core.station.entity.StationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStationStatusRequest {

    @NotNull(message = "Status is required")
    private StationStatus status;

    @Size(max = 500)
    private String reason;
}
