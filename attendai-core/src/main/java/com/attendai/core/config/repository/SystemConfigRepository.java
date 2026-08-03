package com.attendai.core.config.repository;

import com.attendai.core.config.entity.SystemConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    @Query("""
            SELECT c FROM SystemConfig c
            WHERE (:module IS NULL OR c.module = :module)
              AND (:search IS NULL
                   OR LOWER(c.configKey) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY c.module ASC, c.configKey ASC
            """)
    Page<SystemConfig> findByFilters(
            @Param("module") String module,
            @Param("search") String search,
            Pageable pageable);
}
