package com.attendai.core.user.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.user.dto.ChangePasswordRequest;
import com.attendai.core.user.dto.ChangeStatusRequest;
import com.attendai.core.user.dto.CreateUserRequest;
import com.attendai.core.user.dto.UpdateUserRequest;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.entity.User;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.exception.UserAlreadyExistsException;
import com.attendai.core.user.exception.UserNotFoundException;
import com.attendai.core.user.mapper.UserMapper;
import com.attendai.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository  userRepository;
    @Mock UserMapper      userMapper;
    @Mock AuditService    auditService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder, auditService);
    }

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Test
    void createUser_shouldSaveAndReturnResponse_whenValidRequest() {
        when(userRepository.existsByEmailIncludingDeleted("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.findByPersonId(1L)).thenReturn(Optional.empty());

        User saved = buildUser(1L, "john@example.com", "john", UserStatus.ACTIVE);
        when(userRepository.save(any())).thenReturn(saved);

        CreateUserRequest req = createRequest("john@example.com", "john", "Password1", 1L);
        userService.createUser(req);

        verify(userRepository).save(any(User.class));
        verify(auditService).log(any());
    }

    @Test
    void createUser_shouldThrow409_whenEmailAlreadyExists() {
        when(userRepository.existsByEmailIncludingDeleted("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                createRequest("dup@example.com", "user", "Password1", 1L)))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_shouldThrow409_whenUsernameAlreadyExists() {
        when(userRepository.existsByEmailIncludingDeleted("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("dupuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                createRequest("new@example.com", "dupuser", "Password1", 1L)))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void createUser_shouldThrow409_whenPersonAlreadyHasUser() {
        when(userRepository.existsByEmailIncludingDeleted(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.findByPersonId(1L)).thenReturn(Optional.of(buildUser(99L, "x@y.com", "x", UserStatus.ACTIVE)));

        assertThatThrownBy(() -> userService.createUser(
                createRequest("new@example.com", "newuser", "Password1", 1L)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldTransition_whenValidMove() {
        User user = buildUser(1L, "a@b.com", "ab", UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(UserStatus.INACTIVE);
        userService.changeStatus(1L, req);

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(auditService).log(any());
    }

    @Test
    void changeStatus_shouldThrow400_whenTransitionInvalid() {
        // LOCKED → INACTIVE is not allowed
        User user = buildUser(1L, "a@b.com", "ab", UserStatus.LOCKED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(UserStatus.INACTIVE);

        assertThatThrownBy(() -> userService.changeStatus(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void changeStatus_allValidTransitions_shouldSucceed() {
        // ACTIVE → INACTIVE
        assertTransitionValid(UserStatus.ACTIVE, UserStatus.INACTIVE);
        // ACTIVE → SUSPENDED
        assertTransitionValid(UserStatus.ACTIVE, UserStatus.SUSPENDED);
        // ACTIVE → LOCKED
        assertTransitionValid(UserStatus.ACTIVE, UserStatus.LOCKED);
        // INACTIVE → ACTIVE
        assertTransitionValid(UserStatus.INACTIVE, UserStatus.ACTIVE);
        // SUSPENDED → ACTIVE
        assertTransitionValid(UserStatus.SUSPENDED, UserStatus.ACTIVE);
        // LOCKED → ACTIVE
        assertTransitionValid(UserStatus.LOCKED, UserStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    void changePassword_shouldUpdate_whenCurrentPasswordCorrect() {
        String raw = "OldPass1";
        String hash = passwordEncoder.encode(raw);
        User user = buildUser(1L, "a@b.com", "ab", UserStatus.ACTIVE);
        user.setPasswordHash(hash);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(raw);
        req.setNewPassword("NewPass1");

        userService.changePassword(1L, req);

        assertThat(passwordEncoder.matches("NewPass1", user.getPasswordHash())).isTrue();
        assertThat(user.isMustChangePassword()).isFalse();
        verify(auditService).log(any());
    }

    @Test
    void changePassword_shouldThrow400_whenCurrentPasswordWrong() {
        User user = buildUser(1L, "a@b.com", "ab", UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode("CorrectPass1"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WrongPass1");
        req.setNewPassword("NewPass1");

        assertThatThrownBy(() -> userService.changePassword(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_shouldSoftDelete_whenUserExists() {
        User user = buildUser(1L, "a@b.com", "ab", UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deleteUser(1L);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Internal auth API
    // -------------------------------------------------------------------------

    @Test
    void findByEmailForAuth_shouldReturnProjection_whenUserExists() {
        // Build a user with all fields set — note id comes from BaseEntity
        User user = User.builder()
                .personId(1L)
                .email("auth@b.com")
                .username("auth")
                .passwordHash("$2a$04$hash")
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build();
        when(userRepository.findByEmail("auth@b.com")).thenReturn(Optional.of(user));

        Optional<UserAuthProjection> result = userService.findByEmailForAuth("auth@b.com");

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("auth@b.com");
        assertThat(result.get().passwordHash()).isEqualTo("$2a$04$hash");
        assertThat(result.get().status()).isEqualTo("ACTIVE");
        assertThat(result.get().mustChangePassword()).isFalse();
    }

    @Test
    void findByEmailForAuth_shouldReturnEmpty_whenUserNotFound() {
        when(userRepository.findByEmail("notfound@b.com")).thenReturn(Optional.empty());

        assertThat(userService.findByEmailForAuth("notfound@b.com")).isEmpty();
    }

    @Test
    void lockUser_shouldCallRepository() {
        userService.lockUser(5L);
        verify(userRepository).lockUser(5L);
        verify(auditService).log(any());
    }

    @Test
    void updateLastLoginAt_shouldCallRepository() {
        userService.updateLastLoginAt(5L);
        verify(userRepository).updateLastLoginAt(anyLong(), any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertTransitionValid(UserStatus from, UserStatus to) {
        User user = buildUser(1L, "a@b.com", "ab", from);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        ChangeStatusRequest req = new ChangeStatusRequest();
        req.setStatus(to);
        userService.changeStatus(1L, req);

        assertThat(user.getStatus()).isEqualTo(to);
    }

    private User buildUser(Long id, String email, String username, UserStatus status) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setStatus(status);
        user.setPasswordHash(passwordEncoder.encode("Password1"));
        user.setMustChangePassword(true);
        // Simulate saved entity with an ID using reflection-free builder pattern
        return User.builder()
                .personId(1L)
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode("Password1"))
                .status(status)
                .mustChangePassword(true)
                .build();
    }

    private CreateUserRequest createRequest(String email, String username,
                                             String password, Long personId) {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail(email);
        req.setUsername(username);
        req.setTemporaryPassword(password);
        req.setPersonId(personId);
        req.setMustChangePassword(true);
        return req;
    }
}
