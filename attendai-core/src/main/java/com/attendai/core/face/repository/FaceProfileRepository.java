package com.attendai.core.face.repository;

import com.attendai.core.face.entity.FaceProfile;
import com.attendai.core.face.entity.FaceProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link FaceProfile}.
 *
 * The {@code @SQLRestriction("is_deleted = false")} on {@link com.attendai.core.common.entity.SoftDeletableEntity}
 * automatically excludes soft-deleted profiles from all standard queries.
 */
@Repository
public interface FaceProfileRepository extends JpaRepository<FaceProfile, Long> {

    /** Find the non-deleted profile for a person (max 1 enforced at service layer). */
    Optional<FaceProfile> findByPersonId(Long personId);

    /** Find non-deleted profiles with ACTIVE status — used by the recognition engine. */
    @Query("""
            SELECT p FROM FaceProfile p
            WHERE p.status = :status
              AND p.isDeleted = false
            """)
    List<FaceProfile> findAllByStatus(@Param("status") FaceProfileStatus status);
}
