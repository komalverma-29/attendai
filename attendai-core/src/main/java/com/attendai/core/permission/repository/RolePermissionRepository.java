package com.attendai.core.permission.repository;

import com.attendai.core.permission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    boolean existsByPermissionId(Long permissionId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);

    List<RolePermission> findByRoleId(Long roleId);

    /**
     * Single-JOIN query: resolves all permission codes for a user.
     * Used by core-auth during token creation.
     */
    @Query("""
            SELECT DISTINCT p.code FROM Permission p
            JOIN RolePermission rp ON rp.permissionId = p.id
            JOIN com.attendai.core.role.entity.UserRole ur ON ur.roleId = rp.roleId
            WHERE ur.userId = :userId
              AND p.isDeleted = false
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT p.code FROM Permission p
            JOIN RolePermission rp ON rp.permissionId = p.id
            WHERE rp.roleId = :roleId
              AND p.isDeleted = false
            """)
    List<String> findPermissionCodesByRoleId(@Param("roleId") Long roleId);
}
