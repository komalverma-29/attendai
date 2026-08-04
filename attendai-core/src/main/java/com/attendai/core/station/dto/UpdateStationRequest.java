package com.attendai.core.station.dto;

import com.attendai.core.station.entity.StationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateStationRequest {

    @Size(max = 255)
    private String name;

    private StationType type;

    @Size(max = 1000)
    private String description;

    @Size(max = 255)
    private String locationName;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;
}
