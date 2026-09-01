package com.attendai.school.school.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.school.dto.ChangeSchoolStatusRequest;
import com.attendai.school.school.dto.CreateSchoolRequest;
import com.attendai.school.school.dto.SchoolResponse;
import com.attendai.school.school.dto.UpdateSchoolRequest;
import com.attendai.school.school.entity.School;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import com.attendai.school.school.exception.SchoolAlreadyExistsException;
import com.attendai.school.school.exception.SchoolNotFoundException;
import com.attendai.school.school.mapper.SchoolMapper;
import com.attendai.school.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceImplTest {

    @Mock SchoolRepository schoolRepository;
    @Mock SchoolMapper     schoolMapper;
    @Mock AuditService     auditService;

    private SchoolServiceImpl schoolService;

    @BeforeEach
    void setUp() {
        schoolService = new SchoolServiceImpl(schoolRepository, schoolMapper, auditService);
    }

    // -------------------------------------------------------------------------
    // createSchool
    // -------------------------------------------------------------------------

    @Test
    void createSchool_shouldSave_whenValid() {
        when(schoolRepository.existsByName("Sunrise Public School")).thenReturn(false);
        when(schoolRepository.existsByCode(any())).thenReturn(false);
        School saved = buildSchool(1L, "Sunrise Public School", "SPS", SchoolStatus.ACTIVE);
        when(schoolRepository.save(any())).thenReturn(saved);
        when(schoolMapper.toResponse(saved)).thenReturn(buildResponse(saved));

        SchoolResponse result = schoolService.createSchool(buildCreateRequest("Sunrise Public School", "SPS"));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SchoolStatus.ACTIVE);
        verify(schoolRepository).save(any(School.class));
        verify(auditService).log(any());
    }

    @Test
    void createSchool_shouldThrow409_whenNameDuplicate() {
        when(schoolRepository.existsByName("Dup School")).thenReturn(true);

        assertThatThrownBy(() -> schoolService.createSchool(buildCreateRequest("Dup School", null)))
                .isInstanceOf(SchoolAlreadyExistsException.class);
        verify(schoolRepository, never()).save(any());
    }

    @Test
    void createSchool_shouldThrow409_whenCodeDuplicate() {
        when(schoolRepository.existsByName("New School")).thenReturn(false);
        when(schoolRepository.existsByCode("DUP")).thenReturn(true);

        assertThatThrownBy(() -> schoolService.createSchool(buildCreateRequest("New School", "DUP")))
                .isInstanceOf(SchoolAlreadyExistsException.class);
        verify(schoolRepository, never()).save(any());
    }

    @Test
    void createSchool_shouldAutoGenerateCode_whenNotProvided() {
        when(schoolRepository.existsByName("Sunrise Public School")).thenReturn(false);
        when(schoolRepository.existsByCode(any())).thenReturn(false);
        School saved = buildSchool(1L, "Sunrise Public School", "SPS", SchoolStatus.ACTIVE);
        when(schoolRepository.save(any())).thenReturn(saved);
        when(schoolMapper.toResponse(saved)).thenReturn(buildResponse(saved));

        // No code in request — service auto-generates from first letters: S+P+S = "SPS"
        CreateSchoolRequest req = buildCreateRequest("Sunrise Public School", null);
        schoolService.createSchool(req);

        // Verify existsByCode was called (auto-generated code was checked)
        verify(schoolRepository).existsByCode(any());
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldReturn_whenFound() {
        School school = buildSchool(1L, "S", "S", SchoolStatus.ACTIVE);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(buildResponse(school));

        assertThat(schoolService.findById(1L)).isNotNull();
    }

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolService.findById(99L))
                .isInstanceOf(SchoolNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // isActive
    // -------------------------------------------------------------------------

    @Test
    void isActive_shouldReturnTrue_whenSchoolIsActive() {
        when(schoolRepository.findById(1L))
                .thenReturn(Optional.of(buildSchool(1L, "S", "S", SchoolStatus.ACTIVE)));
        assertThat(schoolService.isActive(1L)).isTrue();
    }

    @Test
    void isActive_shouldReturnFalse_whenInactive() {
        when(schoolRepository.findById(1L))
                .thenReturn(Optional.of(buildSchool(1L, "S", "S", SchoolStatus.INACTIVE)));
        assertThat(schoolService.isActive(1L)).isFalse();
    }

    @Test
    void isActive_shouldReturnFalse_whenNotFound() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(schoolService.isActive(99L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // changeStatus — state machine
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldTransition_whenValid() {
        School school = buildSchool(1L, "S", "S", SchoolStatus.ACTIVE);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any())).thenReturn(school);
        when(schoolMapper.toResponse(any())).thenReturn(buildResponse(school));

        ChangeSchoolStatusRequest req = new ChangeSchoolStatusRequest();
        req.setStatus(SchoolStatus.INACTIVE);
        schoolService.changeStatus(1L, req);

        assertThat(school.getStatus()).isEqualTo(SchoolStatus.INACTIVE);
        verify(auditService).log(any());
    }

    @Test
    void changeStatus_shouldThrow400_whenInvalidTransition() {
        // INACTIVE → SUSPENDED is not allowed
        School school = buildSchool(1L, "S", "S", SchoolStatus.INACTIVE);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        ChangeSchoolStatusRequest req = new ChangeSchoolStatusRequest();
        req.setStatus(SchoolStatus.SUSPENDED);

        assertThatThrownBy(() -> schoolService.changeStatus(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void changeStatus_allValidTransitions_shouldSucceed() {
        verifyTransition(SchoolStatus.ACTIVE,    SchoolStatus.INACTIVE);
        verifyTransition(SchoolStatus.ACTIVE,    SchoolStatus.SUSPENDED);
        verifyTransition(SchoolStatus.INACTIVE,  SchoolStatus.ACTIVE);
        verifyTransition(SchoolStatus.SUSPENDED, SchoolStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // deleteSchool
    // -------------------------------------------------------------------------

    @Test
    void deleteSchool_shouldSoftDelete() {
        School school = buildSchool(1L, "S", "S", SchoolStatus.ACTIVE);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any())).thenReturn(school);

        schoolService.deleteSchool(1L);

        assertThat(school.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // updateSchool
    // -------------------------------------------------------------------------

    @Test
    void updateSchool_shouldUpdateFields_whenValid() {
        School school = buildSchool(1L, "Old Name", "OLD", SchoolStatus.ACTIVE);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.existsByName("New Name")).thenReturn(false);
        when(schoolRepository.save(any())).thenReturn(school);
        when(schoolMapper.toResponse(any())).thenReturn(buildResponse(school));

        UpdateSchoolRequest req = new UpdateSchoolRequest();
        req.setName("New Name");
        schoolService.updateSchool(1L, req);

        assertThat(school.getName()).isEqualTo("New Name");
        verify(auditService).log(any());
    }

    @Test
    void updateSchool_shouldThrow409_whenNewNameAlreadyTaken() {
        School school = buildSchool(1L, "Old Name", "OLD", SchoolStatus.ACTIVE);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.existsByName("Taken Name")).thenReturn(true);

        UpdateSchoolRequest req = new UpdateSchoolRequest();
        req.setName("Taken Name");

        assertThatThrownBy(() -> schoolService.updateSchool(1L, req))
                .isInstanceOf(SchoolAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void verifyTransition(SchoolStatus from, SchoolStatus to) {
        School s = buildSchool(1L, "S", "S", from);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(s));
        when(schoolRepository.save(any())).thenReturn(s);
        when(schoolMapper.toResponse(any())).thenReturn(buildResponse(s));

        ChangeSchoolStatusRequest req = new ChangeSchoolStatusRequest();
        req.setStatus(to);
        schoolService.changeStatus(1L, req);
        assertThat(s.getStatus()).isEqualTo(to);
    }

    private School buildSchool(Long id, String name, String code, SchoolStatus status) {
        School s = School.builder()
                .name(name).code(code).type(SchoolType.COMBINED)
                .status(status).addressLine1("123 Main St")
                .city("Mumbai").stateOrProvince("MH").country("IN")
                .build();
        s.setId(id);
        return s;
    }

    private SchoolResponse buildResponse(School s) {
        return SchoolResponse.builder()
                .id(s.getId()).name(s.getName()).code(s.getCode())
                .type(s.getType()).status(s.getStatus())
                .city(s.getCity()).country(s.getCountry())
                .build();
    }

    private CreateSchoolRequest buildCreateRequest(String name, String code) {
        CreateSchoolRequest req = new CreateSchoolRequest();
        req.setName(name);
        req.setCode(code);
        req.setType(SchoolType.COMBINED);
        req.setAddressLine1("123 Main St");
        req.setCity("Mumbai");
        req.setStateOrProvince("Maharashtra");
        req.setCountry("IN");
        return req;
    }
}
