package com.attendai.core.face.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.face.dto.FaceProfileResponse;
import com.attendai.core.face.dto.RecognitionResponse;
import com.attendai.core.face.entity.FaceProfileStatus;
import com.attendai.core.face.exception.FaceProfileAlreadyExistsException;
import com.attendai.core.face.exception.FaceProfileNotFoundException;
import com.attendai.core.face.service.FaceService;
import com.attendai.core.station.config.StationSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FaceController.class)
@Import({SecurityConfig.class, StationSecurityConfig.class})
class FaceControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean FaceService faceService;

    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/core/face";

    // -------------------------------------------------------------------------
    // POST /profiles
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_ENROLL")
    void createProfile_shouldReturn201_whenValid() throws Exception {
        when(faceService.createProfile(any(), anyLong())).thenReturn(buildProfileResponse());

        String body = """
                {"personId": 5}
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post(BASE + "/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_ENROLL")
    void createProfile_shouldReturn400_whenPersonIdMissing() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post(BASE + "/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_ENROLL")
    void createProfile_shouldReturn409_whenAlreadyExists() throws Exception {
        when(faceService.createProfile(any(), anyLong()))
                .thenThrow(new FaceProfileAlreadyExistsException(5L));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post(BASE + "/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    void createProfile_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post(BASE + "/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\": 5}"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /profiles/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_FACE_READ")
    void getProfile_shouldReturn200_whenFound() throws Exception {
        when(faceService.findById(1L)).thenReturn(buildProfileResponse());

        mockMvc.perform(get(BASE + "/profiles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.embeddingVector").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "CORE_FACE_READ")
    void getProfile_shouldReturn404_whenNotFound() throws Exception {
        when(faceService.findById(99L)).thenThrow(new FaceProfileNotFoundException(99L));

        mockMvc.perform(get(BASE + "/profiles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PATCH /profiles/{id}/activate
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_ENROLL")
    void activate_shouldReturn400_whenNoImages() throws Exception {
        when(faceService.activateProfile(anyLong(), anyLong()))
                .thenThrow(new ValidationException("Cannot activate: no images enrolled"));

        mockMvc.perform(patch(BASE + "/profiles/1/activate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // DELETE /profiles/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_DELETE")
    void deleteProfile_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/profiles/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CORE_FACE_READ")
    void deleteProfile_shouldReturn403_whenMissingPermission() throws Exception {
        mockMvc.perform(delete(BASE + "/profiles/1"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /recognize
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_RECOGNIZE")
    void recognize_shouldReturn200_withMatchResult() throws Exception {
        RecognitionResponse resp = RecognitionResponse.builder()
                .matched(true).personId(5L).faceProfileId(1L)
                .confidence(0.95f).livenessCheckPassed(true).build();
        when(faceService.recognize(any(), anyBoolean())).thenReturn(resp);

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "face-bytes".getBytes());

        mockMvc.perform(multipart(BASE + "/recognize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.personId").value(5))
                .andExpect(jsonPath("$.data.confidence").value(0.95));
    }

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FACE_RECOGNIZE")
    void recognize_shouldReturn200_withNoMatchResult() throws Exception {
        RecognitionResponse resp = RecognitionResponse.builder()
                .matched(false).confidence(0.40f).livenessCheckPassed(true).build();
        when(faceService.recognize(any(), anyBoolean())).thenReturn(resp);

        MockMultipartFile file = new MockMultipartFile(
                "image", "face.jpg", "image/jpeg", "face-bytes".getBytes());

        mockMvc.perform(multipart(BASE + "/recognize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(false))
                .andExpect(jsonPath("$.data.personId").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private FaceProfileResponse buildProfileResponse() {
        return FaceProfileResponse.builder()
                .id(1L).personId(5L).status(FaceProfileStatus.PENDING)
                .imageCount(0).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
