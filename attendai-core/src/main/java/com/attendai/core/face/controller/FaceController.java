package com.attendai.core.face.controller;

import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.face.dto.CreateFaceProfileRequest;
import com.attendai.core.face.dto.FaceImageResponse;
import com.attendai.core.face.dto.FaceProfileResponse;
import com.attendai.core.face.dto.RecognitionResponse;
import com.attendai.core.face.service.FaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * REST controller for face profile management and recognition.
 * Base path: /api/v1/core/face
 */
@RestController
@RequestMapping("/api/v1/core/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;

    /** POST /api/v1/core/face/profiles — Create a face profile for a person. */
    @PostMapping("/profiles")
    @PreAuthorize("hasAuthority('CORE_FACE_ENROLL')")
    public ResponseEntity<ApiResponse<FaceProfileResponse>> createProfile(
            @Valid @RequestBody CreateFaceProfileRequest request) {

        Long actorId = requireCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(faceService.createProfile(request, actorId)));
    }

    /** GET /api/v1/core/face/profiles/{id} — Get face profile by ID. */
    @GetMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('CORE_FACE_READ')")
    public ResponseEntity<ApiResponse<FaceProfileResponse>> getProfile(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(faceService.findById(id)));
    }

    /** GET /api/v1/core/face/profiles/person/{personId} — Get active profile by person ID. */
    @GetMapping("/profiles/person/{personId}")
    @PreAuthorize("hasAuthority('CORE_FACE_READ')")
    public ResponseEntity<ApiResponse<FaceProfileResponse>> getProfileByPerson(
            @PathVariable("personId") Long personId) {
        return ResponseEntity.ok(ApiResponse.success(faceService.findByPersonId(personId)));
    }

    /** POST /api/v1/core/face/profiles/{id}/images — Enroll a face image. */
    @PostMapping(value = "/profiles/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CORE_FACE_ENROLL')")
    public ResponseEntity<ApiResponse<FaceImageResponse>> addImage(
            @PathVariable("id") Long profileId,
            @RequestParam(name = "image") MultipartFile image,
            @RequestParam(name = "capturedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime capturedAt) {

        Long actorId = requireCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(faceService.addImage(profileId, image, capturedAt, actorId)));
    }

    /** DELETE /api/v1/core/face/profiles/{id}/images/{imageId} — Remove a face image. */
    @DeleteMapping("/profiles/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('CORE_FACE_ENROLL')")
    public ResponseEntity<Void> removeImage(
            @PathVariable("id") Long profileId,
            @PathVariable("imageId") Long imageId) {

        Long actorId = requireCurrentUserId();
        faceService.removeImage(profileId, imageId, actorId);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/v1/core/face/profiles/{id}/activate — Activate a face profile. */
    @PatchMapping("/profiles/{id}/activate")
    @PreAuthorize("hasAuthority('CORE_FACE_ENROLL')")
    public ResponseEntity<ApiResponse<FaceProfileResponse>> activate(
            @PathVariable("id") Long id) {

        Long actorId = requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(faceService.activateProfile(id, actorId)));
    }

    /** PATCH /api/v1/core/face/profiles/{id}/deactivate — Deactivate a face profile. */
    @PatchMapping("/profiles/{id}/deactivate")
    @PreAuthorize("hasAuthority('CORE_FACE_ENROLL')")
    public ResponseEntity<ApiResponse<FaceProfileResponse>> deactivate(
            @PathVariable("id") Long id) {

        Long actorId = requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(faceService.deactivateProfile(id, actorId)));
    }

    /** DELETE /api/v1/core/face/profiles/{id} — Soft-delete a face profile. */
    @DeleteMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('CORE_FACE_DELETE')")
    public ResponseEntity<Void> deleteProfile(@PathVariable("id") Long id) {
        Long actorId = requireCurrentUserId();
        faceService.deleteProfile(id, actorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/core/face/recognize — Face recognition query.
     * Typically called by stations (ROLE_STATION) or privileged users (CORE_FACE_RECOGNIZE).
     */
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CORE_FACE_RECOGNIZE') or hasAuthority('ROLE_STATION')")
    public ResponseEntity<ApiResponse<RecognitionResponse>> recognize(
            @RequestParam(name = "image") MultipartFile image,
            @RequestParam(name = "livenessCheck", required = false, defaultValue = "false")
            boolean livenessCheck) {

        return ResponseEntity.ok(ApiResponse.success(
                faceService.recognize(image, livenessCheck)));
    }

    private Long requireCurrentUserId() {
        return SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }
}
