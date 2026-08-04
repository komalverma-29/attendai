package com.attendai.core.station.repository;

import com.attendai.core.station.entity.StationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationConfigRepository extends JpaRepository<StationConfig, Long> {

    List<StationConfig> findByStationId(Long stationId);

    Optional<StationConfig> findByStationIdAndConfigKey(Long stationId, String configKey);
}
