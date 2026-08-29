package com.attendai.core.face.repository;

import com.attendai.core.face.entity.FaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link FaceImage}.
 *
 * The {@code @SQLRestriction} on {@link com.attendai.core.common.entity.SoftDeletableEntity}
 * automatically excludes soft-deleted images from all standard queries.
 */
@Repository
public interface FaceImageRepository extends JpaRepository<FaceImage, Long> {

    /** All non-deleted images for a profile. */
    List<FaceImage> findByFaceProfileId(Long faceProfileId);

    /**
     * Count non-deleted images for a profile.
     * Used to validate activation and check if the last image was removed.
     */
    @Query("SELECT COUNT(i) FROM FaceImage i WHERE i.faceProfileId = :profileId AND i.isDeleted = false")
    int countActiveByFaceProfileId(@Param("profileId") Long profileId);

    /**
     * Load all ACTIVE face images across all profiles for recognition.
     * Joins to face_profiles to filter only ACTIVE profiles.
     * Embedding vectors are loaded here — they must NOT be returned to clients.
     */
    @Query("""
            SELECT i FROM FaceImage i
            JOIN FaceProfile p ON p.id = i.faceProfileId
            WHERE p.status = 'ACTIVE'
              AND p.isDeleted = false
              AND i.isDeleted = false
            """)
    List<FaceImage> findAllActiveForRecognition();
}
