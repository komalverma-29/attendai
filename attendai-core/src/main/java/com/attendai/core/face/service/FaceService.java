package com.attendai.core.face.service;

import com.attendai.core.face.dto.CreateFaceProfileRequest;
import com.attendai.core.face.dto.FaceImageResponse;
import com.attendai.core.face.dto.FaceProfileResponse;
import com.attendai.core.face.dto.RecognitionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Core face management service.
 *
 * Manages face profile lifecycle, image enrollment, and recognition queries.
 * All recognition computation is delegated to {@link FaceRecognitionEngine}.
 */
public interface FaceService {

    // Profile management
    FaceProfileResponse createProfile(CreateFaceProfileRequest request, Long actorUserId);
    FaceProfileResponse findById(Long profileId);
    FaceProfileResponse findByPersonId(Long personId);
    FaceProfileResponse activateProfile(Long profileId, Long actorUserId);
    FaceProfileResponse deactivateProfile(Long profileId, Long actorUserId);
    void deleteProfile(Long profileId, Long actorUserId);

    // Image management
    FaceImageResponse addImage(Long profileId, MultipartFile imageFile,
                               LocalDateTime capturedAt, Long actorUserId);
    void removeImage(Long profileId, Long imageId, Long actorUserId);

    // Recognition
    RecognitionResponse recognize(MultipartFile imageFile, boolean livenessCheck);
}
