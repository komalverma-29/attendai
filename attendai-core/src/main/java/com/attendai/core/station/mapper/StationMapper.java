package com.attendai.core.station.mapper;

import com.attendai.core.station.dto.StationResponse;
import com.attendai.core.station.dto.StationSummaryResponse;
import com.attendai.core.station.entity.Station;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Station entities.
 *
 * The {@code apiKeyHash} field is intentionally absent from both response DTOs
 * and is never mapped — the hash must never be exposed through any API response.
 */
@Mapper(componentModel = "spring")
public interface StationMapper {

    StationResponse       toResponse(Station station);
    StationSummaryResponse toSummaryResponse(Station station);
}
