package com.attendai.core.station.dto;

import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StationSummaryResponse {
    private final Long          id;
    private final String        name;
    private final StationType   type;
    private final StationStatus status;
    private final String        locationName;
}
