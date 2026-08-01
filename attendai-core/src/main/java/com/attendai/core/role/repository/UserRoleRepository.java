package com.attendai.core.role.repository;

import com.attendai.core.role.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByRoleId(Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    /** Returns role codes for a user — used by core-auth for JWT building. */
    @Query("""
            SELECT r.code FROM Role r
            JOIN UserRole ur ON ur.roleId = r.id
            WHERE ur.userId = :userId
              AND r.isDeleted = false
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
