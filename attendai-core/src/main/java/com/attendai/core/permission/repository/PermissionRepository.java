package com.attendai.core.permission.repository;

import com.attendai.core.permission.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            SELECT p FROM Permission p
            WHERE (:module IS NULL OR p.module = :module)
              AND (:search IS NULL
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.module ASC, p.code ASC
            """)
    Page<Permission> findByFilters(@Param("module") String module,
                                   @Param("search") String search,
                                   Pageable pageable);
}
