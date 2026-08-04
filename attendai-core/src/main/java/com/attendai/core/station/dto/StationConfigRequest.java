package com.attendai.core.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StationConfigRequest {

    @NotBlank(message = "Config key is required")
    @Size(max = 100)
    private String key;

    @NotBlank(message = "Config value is required")
    @Size(max = 1000)
    private String value;
}
