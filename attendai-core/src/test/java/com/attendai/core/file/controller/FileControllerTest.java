package com.attendai.core.file.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.file.dto.FileMetadataResponse;
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.entity.FileVisibility;
import com.attendai.core.file.exception.FileNotFoundException;
import com.attendai.core.file.service.FileService;
import com.attendai.core.station.config.StationSecurityConfig;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@Import({SecurityConfig.class, StationSecurityConfig.class})
class FileControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean FileService fileService;

    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/core/files";

    // -------------------------------------------------------------------------
    // POST — upload
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FILE_UPLOAD")
    void upload_shouldReturn201_whenValidFile() throws Exception {
        FileUploadResponse resp = FileUploadResponse.builder()
                .id(1L).originalName("photo.jpg").contentType("image/jpeg")
                .sizeBytes(1024L).visibility(FileVisibility.PRIVATE)
                .createdAt(LocalDateTime.now()).build();
        when(fileService.upload(any(), any(), any(), anyLong())).thenReturn(resp);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart(BASE).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist());
    }

    @Test
    void upload_shouldReturn401_whenUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "bytes".getBytes());

        mockMvc.perform(multipart(BASE).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FILE_READ")
    void upload_shouldReturn403_whenMissingUploadPermission() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "bytes".getBytes());

        mockMvc.perform(multipart(BASE).file(file))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /{id} — metadata
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FILE_READ")
    void getMetadata_shouldReturn200_andNotExposeStorageKey() throws Exception {
        FileMetadataResponse resp = FileMetadataResponse.builder()
                .id(1L).originalName("photo.jpg").contentType("image/jpeg")
                .sizeBytes(1024L).visibility(FileVisibility.PRIVATE)
                .uploadedByUserId(1L).createdAt(LocalDateTime.now()).build();

        when(fileService.getMetadata(anyLong(), anyLong(), anyBoolean())).thenReturn(resp);

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FILE_READ")
    void getMetadata_shouldReturn404_whenNotFound() throws Exception {
        when(fileService.getMetadata(anyLong(), any(), anyBoolean()))
                .thenThrow(new FileNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // DELETE /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "CORE_FILE_DELETE")
    void deleteFile_shouldReturn204_whenOwner() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteFile_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET — list own files
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1")
    void listOwnFiles_shouldReturn200() throws Exception {
        when(fileService.listOwnFiles(anyLong(), isNull(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
