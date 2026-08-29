package com.attendai.core.face.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ExternalServiceException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.face.config.FaceProperties;
import com.attendai.core.face.dto.CreateFaceProfileRequest;
import com.attendai.core.face.dto.FaceImageResponse;
import com.attendai.core.face.dto.FaceProfileResponse;
import com.attendai.core.face.dto.RecognitionResponse;
import com.attendai.core.face.entity.FaceImage;
import com.attendai.core.face.entity.FaceProfile;
import com.attendai.core.face.entity.FaceProfileStatus;
import com.attendai.core.face.exception.FaceProfileAlreadyExistsException;
import com.attendai.core.face.exception.FaceProfileNotFoundException;
import com.attendai.core.face.mapper.FaceMapper;
import com.attendai.core.face.repository.FaceImageRepository;
import com.attendai.core.face.repository.FaceProfileRepository;
import com.attendai.core.file.entity.FileVisibility;
import com.attendai.core.file.service.FileService;
import com.attendai.core.person.service.PersonService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceServiceImpl implements FaceService {

    private final FaceProfileRepository   faceProfileRepository;
    private final FaceImageRepository     faceImageRepository;
    private final FaceRecognitionEngine   recognitionEngine;
    private final FaceMapper              faceMapper;
    private final FaceProperties          faceProperties;
    private final PersonService           personService;
    private final FileService             fileService;
    private final AuditService            auditService;
    private final ObjectMapper            objectMapper;

    // -------------------------------------------------------------------------
    // Profile management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FaceProfileResponse createProfile(CreateFaceProfileRequest request, Long actorUserId) {
        if (!personService.existsById(request.getPersonId())) {
            throw new com.attendai.core.common.exception.ResourceNotFoundException(
                    "Person with id " + request.getPersonId() + " was not found");
        }
        faceProfileRepository.findByPersonId(request.getPersonId()).ifPresent(existing -> {
            throw new FaceProfileAlreadyExistsException(request.getPersonId());
        });

        FaceProfile profile = FaceProfile.builder()
                .personId(request.getPersonId())
                .status(FaceProfileStatus.PENDING)
                .notes(request.getNotes())
                .build();

        FaceProfile saved = faceProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FACE_PROFILE_CREATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("FaceProfile")
                .resourceId(String.valueOf(saved.getId()))
                .actorUserId(actorUserId)
                .details("{\"personId\":" + saved.getPersonId() + "}")
                .build());

        log.info("Face profile created | profileId={} personId={}", saved.getId(), saved.getPersonId());
        return faceMapper.toProfileResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FaceProfileResponse findById(Long profileId) {
        return faceMapper.toProfileResponse(requireProfile(profileId));
    }

    @Override
    @Transactional(readOnly = true)
    public FaceProfileResponse findByPersonId(Long personId) {
        return faceProfileRepository.findByPersonId(personId)
                .map(faceMapper::toProfileResponse)
                .orElseThrow(() -> new FaceProfileNotFoundException(personId));
    }

    @Override
    @Transactional
    public FaceProfileResponse activateProfile(Long profileId, Long actorUserId) {
        FaceProfile profile = requireProfile(profileId);

        if (profile.getImageCount() == 0) {
            throw new ValidationException(
                    "Cannot activate face profile with id " + profileId + ": no images enrolled");
        }

        profile.setStatus(FaceProfileStatus.ACTIVE);
        FaceProfile saved = faceProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FACE_PROFILE_ACTIVATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("FaceProfile")
                .resourceId(String.valueOf(profileId))
                .actorUserId(actorUserId)
                .build());

        return faceMapper.toProfileResponse(saved);
    }

    @Override
    @Transactional
    public FaceProfileResponse deactivateProfile(Long profileId, Long actorUserId) {
        FaceProfile profile = requireProfile(profileId);
        profile.setStatus(FaceProfileStatus.INACTIVE);
        FaceProfile saved = faceProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FACE_PROFILE_DEACTIVATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("FaceProfile")
                .resourceId(String.valueOf(profileId))
                .actorUserId(actorUserId)
                .build());

        return faceMapper.toProfileResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProfile(Long profileId, Long actorUserId) {
        FaceProfile profile = requireProfile(profileId);

        // Soft-delete all images first
        faceImageRepository.findByFaceProfileId(profileId).forEach(img -> {
            img.softDelete();
            faceImageRepository.save(img);
        });

        profile.softDelete();
        profile.setImageCount(0);
        faceProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FACE_PROFILE_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("FaceProfile")
                .resourceId(String.valueOf(profileId))
                .actorUserId(actorUserId)
                .details("{\"personId\":" + profile.getPersonId() + "}")
                .build());

        log.info("Face profile deleted | profileId={}", profileId);
    }

    // -------------------------------------------------------------------------
    // Image management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FaceImageResponse addImage(Long profileId, MultipartFile imageFile,
                                       LocalDateTime capturedAt, Long actorUserId) {
        FaceProfile profile = requireProfile(profileId);

        if (profile.getImageCount() >= faceProperties.getMaxImagesPerProfile()) {
            throw new ValidationException(
                    "Face profile " + profileId + " already has the maximum of "
                    + faceProperties.getMaxImagesPerProfile() + " images");
        }

        // 1. Store image in core-file
        com.attendai.core.file.dto.FileUploadResponse fileResponse =
                fileService.upload(imageFile, FileVisibility.PRIVATE, "core-face", actorUserId);

        // 2. Extract embedding using the recognition engine
        byte[] imageBytes;
        try {
            imageBytes = imageFile.getBytes();
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to read image bytes: " + e.getMessage(), e);
        }

        FaceEmbedding embedding;
        try {
            embedding = recognitionEngine.extractEmbedding(imageBytes);
        } catch (Exception e) {
            // Clean up the uploaded file if embedding extraction fails
            fileService.deleteById(fileResponse.getId());
            throw new ExternalServiceException(
                    "Face recognition engine failed to extract embedding: " + e.getMessage(), e);
        }

        // 3. Serialize embedding to JSON and persist
        String embeddingJson = serializeEmbedding(embedding.getVector());

        FaceImage image = FaceImage.builder()
                .faceProfileId(profileId)
                .fileId(fileResponse.getId())
                .embeddingVector(embeddingJson)
                .capturedAt(capturedAt)
                .build();

        FaceImage saved = faceImageRepository.save(image);

        // 4. Update denormalized image count
        profile.setImageCount(profile.getImageCount() + 1);
        faceProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FACE_IMAGE_ENROLLED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("FaceImage")
                .resourceId(String.valueOf(saved.getId()))
                .actorUserId(actorUserId)
                .details("{\"profileId\":" + profileId + "}")
                .build());

        log.info("Face image enrolled | imageId={} profileId={}", saved.getId(), profileId);
        return faceMapper.toImageResponse(saved);
    }

    @Override
    @Transactional
    public void removeImage(Long profileId, Long imageId, Long actorUserId) {
        requireProfile(profileId);

        FaceImage image = faceImageRepository.findById(imageId)
                .orElseThrow(() -> new com.attendai.core.common.exception.ResourceNotFoundException(
                        "Face image with id " + imageId + " was not found"));

        if (!image.getFaceProfileId().equals(profileId)) {
            throw new ValidationException(
                    "Face image " + imageId + " does not belong to profile " + profileId);
        }

        image.softDelete();
        faceImageRepository.save(image);

        // Decrement count and check if profile should revert to PENDING
        FaceProfile profile = requireProfile(profileId);
        int remaining = faceImageRepository.countActiveByFaceProfileId(profileId);
        profile.setImageCount(remaining);
        if (remaining == 0 && profile.getStatus() == FaceProfileStatus.ACTIVE) {
            profile.setStatus(FaceProfileStatus.PENDING);
            log.info("Face profile reverted to PENDING (last image removed) | profileId={}", profileId);
        }
        faceProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FACE_IMAGE_REMOVED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("FaceImage")
                .resourceId(String.valueOf(imageId))
                .actorUserId(actorUserId)
                .details("{\"profileId\":" + profileId + "}")
                .build());
    }

    // -------------------------------------------------------------------------
    // Recognition
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public RecognitionResponse recognize(MultipartFile imageFile, boolean livenessCheck) {
        byte[] imageBytes;
        try {
            imageBytes = imageFile.getBytes();
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to read image bytes: " + e.getMessage(), e);
        }

        // Liveness check (optional)
        boolean livenessCheckPassed = true;
        if (livenessCheck || faceProperties.isLivenessCheckEnabled()) {
            try {
                livenessCheckPassed = recognitionEngine.isLive(imageBytes);
            } catch (Exception e) {
                throw new ExternalServiceException(
                        "Liveness check failed: " + e.getMessage(), e);
            }
            if (!livenessCheckPassed) {
                auditService.log(AuditEventRequest.builder()
                        .actionCode("FACE_RECOGNITION_NO_MATCH")
                        .module(AttendAIConstants.MODULE_CORE)
                        .details("{\"reason\":\"liveness_check_failed\",\"confidence\":0.0}")
                        .build());
                return RecognitionResponse.builder()
                        .matched(false).confidence(0.0f).livenessCheckPassed(false).build();
            }
        }

        // Load all ACTIVE embeddings
        List<FaceImage> activeImages = faceImageRepository.findAllActiveForRecognition();
        if (activeImages.isEmpty()) {
            return RecognitionResponse.builder()
                    .matched(false).confidence(0.0f).livenessCheckPassed(true).build();
        }

        // Build candidate list with back-references to profile/person
        List<FaceEmbedding> candidates = buildCandidates(activeImages);

        // Find best match
        RecognitionResult result;
        try {
            result = recognitionEngine.findBestMatch(imageBytes, candidates);
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "Face recognition engine failed: " + e.getMessage(), e);
        }

        boolean matched = result.isMatched()
                && result.getConfidence() >= faceProperties.getRecognitionThreshold();

        if (matched) {
            auditService.log(AuditEventRequest.builder()
                    .actionCode("FACE_RECOGNITION_MATCH")
                    .module(AttendAIConstants.MODULE_CORE)
                    .details("{\"personId\":" + result.getPersonId()
                            + ",\"confidence\":" + result.getConfidence() + "}")
                    .build());
        } else {
            auditService.log(AuditEventRequest.builder()
                    .actionCode("FACE_RECOGNITION_NO_MATCH")
                    .module(AttendAIConstants.MODULE_CORE)
                    .details("{\"confidence\":" + result.getConfidence() + "}")
                    .build());
        }

        return RecognitionResponse.builder()
                .matched(matched)
                .personId(matched ? result.getPersonId() : null)
                .faceProfileId(matched ? result.getFaceProfileId() : null)
                .confidence(result.getConfidence())
                .livenessCheckPassed(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FaceProfile requireProfile(Long id) {
        return faceProfileRepository.findById(id)
                .orElseThrow(() -> new FaceProfileNotFoundException(id));
    }

    private String serializeEmbedding(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize embedding vector", e);
        }
    }

    private List<FaceEmbedding> buildCandidates(List<FaceImage> images) {
        List<FaceEmbedding> candidates = new ArrayList<>();
        for (FaceImage img : images) {
            try {
                float[] vector = objectMapper.readValue(img.getEmbeddingVector(), float[].class);
                FaceProfile profile = faceProfileRepository.findById(img.getFaceProfileId())
                        .orElse(null);
                if (profile == null) continue;

                candidates.add(FaceEmbedding.builder()
                        .vector(vector)
                        .faceProfileId(img.getFaceProfileId())
                        .faceImageId(img.getId())
                        .personId(profile.getPersonId())
                        .build());
            } catch (Exception e) {
                log.warn("Skipping face image {} — failed to deserialize embedding: {}",
                        img.getId(), e.getMessage());
            }
        }
        return candidates;
    }
}
