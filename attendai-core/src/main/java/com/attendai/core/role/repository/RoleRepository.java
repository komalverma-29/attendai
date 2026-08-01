package com.attendai.core.role.repository;

import com.attendai.core.role.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            SELECT r FROM Role r
            WHERE (:search IS NULL
                   OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.name ASC
            """)
    Page<Role> findBySearch(@Param("search") String search, Pageable pageable);
}
