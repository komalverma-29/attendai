package com.attendai.core.user.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.user.dto.ChangeStatusRequest;
import com.attendai.core.user.dto.CreateUserRequest;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.exception.UserNotFoundException;
import com.attendai.core.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    // MockBeans needed by SecurityConfig
    @MockBean com.attendai.core.auth.service.JwtService jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties securityProperties;

    private static final String BASE = "/api/v1/core/users";

    // -------------------------------------------------------------------------
    // POST /api/v1/core/users
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_USER_CREATE")
    void createUser_shouldReturn201_whenValidRequest() throws Exception {
        UserResponse resp = buildUserResponse();
        when(userService.createUser(any())).thenReturn(resp);

        CreateUserRequest req = new CreateUserRequest();
        req.setPersonId(1L);
        req.setEmail("john@example.com");
        req.setUsername("john");
        req.setTemporaryPassword("Password1");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @WithMockUser(authorities = "CORE_USER_CREATE")
    void createUser_shouldReturn400_whenEmailMissing() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setPersonId(1L);
        req.setUsername("john");
        req.setTemporaryPassword("Password1");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createUser_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CORE_USER_READ")
    void createUser_shouldReturn403_whenWrongPermission() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setPersonId(1L);
        req.setEmail("j@e.com");
        req.setUsername("j");
        req.setTemporaryPassword("Password1");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/core/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_USER_READ")
    void getUser_shouldReturn200_whenFound() throws Exception {
        when(userService.findById(1L)).thenReturn(buildUserResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "CORE_USER_READ")
    void getUser_shouldReturn404_whenNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/core/users/{id}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_USER_UPDATE")
    void changeStatus_shouldReturn400_whenInvalidTransition() throws Exception {
        when(userService.changeStatus(anyLong(), any()))
                .thenThrow(new ValidationException("Cannot transition user status from LOCKED to INACTIVE"));

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(UserStatus.INACTIVE);

        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/core/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_USER_DELETE")
    void deleteUser_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CORE_USER_READ")
    void deleteUser_shouldReturn403_whenNoDeletePermission() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private UserResponse buildUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .personId(1L)
                .email("john@example.com")
                .username("john")
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
