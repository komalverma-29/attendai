package com.attendai.core.face.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.face.config.FaceProperties;
import com.attendai.core.face.dto.CreateFaceProfileRequest;
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
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.entity.FileVisibility;
import com.attendai.core.file.service.FileService;
import com.attendai.core.person.service.PersonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceServiceImplTest {

    @Mock FaceProfileRepository  faceProfileRepository;
    @Mock FaceImageRepository    faceImageRepository;
    @Mock FaceRecognitionEngine  recognitionEngine;
    @Mock FaceMapper             faceMapper;
    @Mock PersonService          personService;
    @Mock FileService            fileService;
    @Mock AuditService           auditService;

    private FaceProperties   faceProperties;
    private FaceServiceImpl  faceService;
    private ObjectMapper     objectMapper;

    @BeforeEach
    void setUp() {
        faceProperties = new FaceProperties();
        objectMapper   = new ObjectMapper();
        faceService    = new FaceServiceImpl(
                faceProfileRepository, faceImageRepository, recognitionEngine,
                faceMapper, faceProperties, personService, fileService,
                auditService, objectMapper);
    }

    // -------------------------------------------------------------------------
    // createProfile
    // -------------------------------------------------------------------------

    @Test
    void createProfile_shouldSaveAndReturn_whenPersonExistsAndNoExistingProfile() {
        when(personService.existsById(1L)).thenReturn(true);
        when(faceProfileRepository.findByPersonId(1L)).thenReturn(Optional.empty());
        FaceProfile saved = buildProfile(10L, 1L, FaceProfileStatus.PENDING, 0);
        when(faceProfileRepository.save(any())).thenReturn(saved);
        when(faceMapper.toProfileResponse(saved)).thenReturn(buildProfileResponse(saved));

        CreateFaceProfileRequest req = new CreateFaceProfileRequest();
        req.setPersonId(1L);

        FaceProfileResponse result = faceService.createProfile(req, 99L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FaceProfileStatus.PENDING);
        verify(faceProfileRepository).save(any(FaceProfile.class));
        verify(auditService).log(any());
    }

    @Test
    void createProfile_shouldThrow409_whenProfileAlreadyExists() {
        when(personService.existsById(1L)).thenReturn(true);
        when(faceProfileRepository.findByPersonId(1L))
                .thenReturn(Optional.of(buildProfile(5L, 1L, FaceProfileStatus.ACTIVE, 2)));

        CreateFaceProfileRequest req = new CreateFaceProfileRequest();
        req.setPersonId(1L);

        assertThatThrownBy(() -> faceService.createProfile(req, 99L))
                .isInstanceOf(FaceProfileAlreadyExistsException.class);
        verify(faceProfileRepository, never()).save(any());
    }

    @Test
    void createProfile_shouldThrow404_whenPersonNotFound() {
        when(personService.existsById(1L)).thenReturn(false);

        CreateFaceProfileRequest req = new CreateFaceProfileRequest();
        req.setPersonId(1L);

        assertThatThrownBy(() -> faceService.createProfile(req, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // activateProfile
    // -------------------------------------------------------------------------

    @Test
    void activateProfile_shouldSetActive_whenImagesEnrolled() {
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.PENDING, 3);
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(faceProfileRepository.save(any())).thenReturn(profile);
        when(faceMapper.toProfileResponse(any())).thenReturn(buildProfileResponse(profile));

        faceService.activateProfile(1L, 99L);

        assertThat(profile.getStatus()).isEqualTo(FaceProfileStatus.ACTIVE);
        verify(auditService).log(any());
    }

    @Test
    void activateProfile_shouldThrow400_whenNoImagesEnrolled() {
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.PENDING, 0);
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> faceService.activateProfile(1L, 99L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no images enrolled");
    }

    // -------------------------------------------------------------------------
    // deactivateProfile
    // -------------------------------------------------------------------------

    @Test
    void deactivateProfile_shouldSetInactive() {
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.ACTIVE, 2);
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(faceProfileRepository.save(any())).thenReturn(profile);
        when(faceMapper.toProfileResponse(any())).thenReturn(buildProfileResponse(profile));

        faceService.deactivateProfile(1L, 99L);

        assertThat(profile.getStatus()).isEqualTo(FaceProfileStatus.INACTIVE);
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // removeImage — last image triggers PENDING revert
    // -------------------------------------------------------------------------

    @Test
    void removeImage_shouldRevertToPending_whenLastImageRemoved() {
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.ACTIVE, 1);
        FaceImage image = buildImage(10L, 1L);

        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(faceImageRepository.findById(10L)).thenReturn(Optional.of(image));
        when(faceImageRepository.countActiveByFaceProfileId(1L)).thenReturn(0);
        when(faceImageRepository.save(any())).thenReturn(image);
        when(faceProfileRepository.save(any())).thenReturn(profile);

        faceService.removeImage(1L, 10L, 99L);

        assertThat(profile.getStatus()).isEqualTo(FaceProfileStatus.PENDING);
        assertThat(profile.getImageCount()).isZero();
    }

    @Test
    void removeImage_shouldNotRevertStatus_whenImagesStillRemain() {
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.ACTIVE, 3);
        FaceImage image = buildImage(10L, 1L);

        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(faceImageRepository.findById(10L)).thenReturn(Optional.of(image));
        when(faceImageRepository.countActiveByFaceProfileId(1L)).thenReturn(2);
        when(faceImageRepository.save(any())).thenReturn(image);
        when(faceProfileRepository.save(any())).thenReturn(profile);

        faceService.removeImage(1L, 10L, 99L);

        assertThat(profile.getStatus()).isEqualTo(FaceProfileStatus.ACTIVE);
        assertThat(profile.getImageCount()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Recognition
    // -------------------------------------------------------------------------

    @Test
    void recognize_shouldReturnNoMatch_whenNoActiveEmbeddings() {
        when(faceImageRepository.findAllActiveForRecognition()).thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "bytes".getBytes());

        RecognitionResponse result = faceService.recognize(file, false);

        assertThat(result.isMatched()).isFalse();
        assertThat(result.isLivenessCheckPassed()).isTrue();
    }

    @Test
    void recognize_shouldReturnNoMatch_whenConfidenceBelowThreshold() {
        faceProperties.setRecognitionThreshold(0.85f);

        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.ACTIVE, 1);
        FaceImage image = buildImage(10L, 1L);
        image.setEmbeddingVector("[0.1, 0.2]");

        when(faceImageRepository.findAllActiveForRecognition()).thenReturn(List.of(image));
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(recognitionEngine.findBestMatch(any(), any()))
                .thenReturn(RecognitionResult.builder()
                        .matched(true)
                        .confidence(0.60f) // below 0.85 threshold
                        .personId(5L)
                        .faceProfileId(1L)
                        .build());

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "bytes".getBytes());

        RecognitionResponse result = faceService.recognize(file, false);

        assertThat(result.isMatched()).isFalse();
        assertThat(result.getConfidence()).isEqualTo(0.60f);
    }

    @Test
    void recognize_shouldReturnMatch_whenConfidenceAtOrAboveThreshold() {
        faceProperties.setRecognitionThreshold(0.85f);

        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.ACTIVE, 1);
        FaceImage image = buildImage(10L, 1L);
        image.setEmbeddingVector("[0.1, 0.2]");

        when(faceImageRepository.findAllActiveForRecognition()).thenReturn(List.of(image));
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(recognitionEngine.findBestMatch(any(), any()))
                .thenReturn(RecognitionResult.builder()
                        .matched(true)
                        .confidence(0.92f) // above threshold
                        .personId(5L)
                        .faceProfileId(1L)
                        .build());

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "bytes".getBytes());

        RecognitionResponse result = faceService.recognize(file, false);

        assertThat(result.isMatched()).isTrue();
        assertThat(result.getPersonId()).isEqualTo(5L);
        assertThat(result.getFaceProfileId()).isEqualTo(1L);
        assertThat(result.getConfidence()).isEqualTo(0.92f);
    }

    @Test
    void recognize_shouldReturnNoMatch_whenLivenessCheckFails() {
        when(recognitionEngine.isLive(any())).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "bytes".getBytes());

        RecognitionResponse result = faceService.recognize(file, true);

        assertThat(result.isMatched()).isFalse();
        assertThat(result.isLivenessCheckPassed()).isFalse();
    }

    @Test
    void findById_shouldThrow404_whenProfileNotFound() {
        when(faceProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> faceService.findById(99L))
                .isInstanceOf(FaceProfileNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // addImage — image limit
    // -------------------------------------------------------------------------

    @Test
    void addImage_shouldThrow400_whenImageLimitExceeded() {
        faceProperties.setMaxImagesPerProfile(10);
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.PENDING, 10);
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "bytes".getBytes());

        assertThatThrownBy(() -> faceService.addImage(1L, file, null, 99L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum");
    }

    // -------------------------------------------------------------------------
    // deleteProfile
    // -------------------------------------------------------------------------

    @Test
    void deleteProfile_shouldSoftDeleteProfileAndImages() {
        FaceProfile profile = buildProfile(1L, 5L, FaceProfileStatus.ACTIVE, 1);
        FaceImage image = buildImage(10L, 1L);
        when(faceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(faceImageRepository.findByFaceProfileId(1L)).thenReturn(List.of(image));
        when(faceImageRepository.save(any())).thenReturn(image);
        when(faceProfileRepository.save(any())).thenReturn(profile);

        faceService.deleteProfile(1L, 99L);

        assertThat(profile.isDeleted()).isTrue();
        assertThat(image.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FaceProfile buildProfile(Long id, Long personId,
                                      FaceProfileStatus status, int imageCount) {
        FaceProfile p = FaceProfile.builder()
                .personId(personId)
                .status(status)
                .imageCount(imageCount)
                .build();
        p.setId(id);
        return p;
    }

    private FaceImage buildImage(Long id, Long profileId) {
        FaceImage i = FaceImage.builder()
                .faceProfileId(profileId)
                .fileId(100L)
                .embeddingVector("[0.1, 0.2, 0.3]")
                .build();
        i.setId(id);
        return i;
    }

    private FaceProfileResponse buildProfileResponse(FaceProfile p) {
        return FaceProfileResponse.builder()
                .id(p.getId())
                .personId(p.getPersonId())
                .status(p.getStatus())
                .imageCount(p.getImageCount())
                .build();
    }
}
