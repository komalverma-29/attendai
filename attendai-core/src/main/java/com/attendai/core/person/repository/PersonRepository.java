package com.attendai.core.person.repository;

import com.attendai.core.person.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Person entities.
 *
 * The {@code @SQLRestriction("is_deleted = false")} inherited from
 * {@link com.attendai.core.common.entity.SoftDeletableEntity} automatically
 * excludes soft-deleted persons from all standard queries.
 *
 * Email uniqueness is checked across ALL persons (including soft-deleted)
 * via a dedicated query to prevent reuse after soft deletion.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Checks whether an email exists across ALL persons including soft-deleted.
     * Used for uniqueness enforcement on create and update.
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.email = :email")
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    /**
     * Checks email uniqueness excluding a specific person ID.
     * Used when updating a person to allow them to keep their current email.
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p WHERE p.email = :email AND p.id != :excludeId")
    boolean existsByEmailExcludingId(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * Full-text search across first name, last name, email, and phone.
     * Case-insensitive partial match. Respects the soft-delete filter.
     */
    @Query("""
            SELECT p FROM Person p
            WHERE (:search IS NULL
                   OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.email)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR p.phone            LIKE CONCAT('%', :search, '%'))
            ORDER BY p.lastName ASC, p.firstName ASC
            """)
    Page<Person> search(@Param("search") String search, Pageable pageable);
}
