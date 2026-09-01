package com.attendai.school.administrator.repository;

import com.attendai.school.administrator.entity.AdministratorStatus;
import com.attendai.school.administrator.entity.SchoolAdministrator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolAdministratorRepository extends JpaRepository<SchoolAdministrator, Long> {

    /** Enforce one-admin-per-person-per-school rule. */
    boolean existsByPersonIdAndSchoolId(Long personId, Long schoolId);

    /** Enforce one-user-per-admin rule. */
    Optional<SchoolAdministrator> findByUserId(Long userId);

    /** Used for the last-admin guard: count active admins in a school. */
    long countBySchoolIdAndStatus(Long schoolId, AdministratorStatus status);

    /** Paginated list for a school with optional status filter. */
    @Query("""
            SELECT a FROM SchoolAdministrator a
            WHERE a.schoolId = :schoolId
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.createdAt DESC
            """)
    Page<SchoolAdministrator> findBySchoolIdAndStatus(
            @Param("schoolId") Long schoolId,
            @Param("status")   AdministratorStatus status,
            Pageable pageable);

    List<SchoolAdministrator> findBySchoolId(Long schoolId);
}
