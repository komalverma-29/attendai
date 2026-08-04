package com.attendai.core.station.dto;

import com.attendai.core.station.entity.StationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateStationRequest {

    @NotBlank(message = "Station name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Station type is required")
    private StationType type;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 255, message = "Location name must not exceed 255 characters")
    private String locationName;

    @DecimalMin(value = "-90.0",  message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0",   message = "Latitude must be between -90.0 and 90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0",  message = "Longitude must be between -180.0 and 180.0")
    private BigDecimal longitude;
}
