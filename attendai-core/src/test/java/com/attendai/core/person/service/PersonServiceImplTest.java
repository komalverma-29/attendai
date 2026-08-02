package com.attendai.core.person.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.dto.CreatePersonRequest;
import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.dto.UpdatePersonRequest;
import com.attendai.core.person.entity.Person;
import com.attendai.core.person.exception.PersonNotFoundException;
import com.attendai.core.person.mapper.PersonMapper;
import com.attendai.core.person.repository.PersonRepository;
import com.attendai.core.user.entity.User;
import com.attendai.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class PersonServiceImplTest {

    @Mock PersonRepository personRepository;
    @Mock UserRepository   userRepository;
    @Mock PersonMapper     personMapper;
    @Mock AuditService     auditService;

    private PersonServiceImpl personService;

    @BeforeEach
    void setUp() {
        personService = new PersonServiceImpl(
                personRepository, userRepository, personMapper, auditService);
    }

    // -------------------------------------------------------------------------
    // createPerson
    // -------------------------------------------------------------------------

    @Test
    void createPerson_shouldSaveAndReturnResponse_whenValidRequest() {
        when(personRepository.existsByEmailIncludingDeleted("john@example.com")).thenReturn(false);
        Person saved = buildPerson();
        when(personRepository.save(any())).thenReturn(saved);
        when(personMapper.toResponse(saved)).thenReturn(buildPersonResponse());

        CreatePersonRequest req = buildCreateRequest("john@example.com");
        PersonResponse result = personService.createPerson(req);

        assertThat(result).isNotNull();
        verify(personRepository).save(any(Person.class));
        verify(auditService).log(any());
    }

    @Test
    void createPerson_shouldNormaliseEmailToLowercase() {
        when(personRepository.existsByEmailIncludingDeleted("john@example.com")).thenReturn(false);
        when(personRepository.save(any())).thenReturn(buildPerson());
        when(personMapper.toResponse(any())).thenReturn(buildPersonResponse());

        CreatePersonRequest req = buildCreateRequest("John@Example.COM");
        personService.createPerson(req);

        // Email should have been lowercased before the uniqueness check
        verify(personRepository).existsByEmailIncludingDeleted("john@example.com");
    }

    @Test
    void createPerson_shouldNotCheckEmail_whenEmailIsNull() {
        when(personRepository.save(any())).thenReturn(buildPerson());
        when(personMapper.toResponse(any())).thenReturn(buildPersonResponse());

        CreatePersonRequest req = buildCreateRequest(null);
        personService.createPerson(req);

        verify(personRepository, never()).existsByEmailIncludingDeleted(anyString());
    }

    @Test
    void createPerson_shouldThrow409_whenEmailDuplicate() {
        when(personRepository.existsByEmailIncludingDeleted("dup@example.com")).thenReturn(true);

        CreatePersonRequest req = buildCreateRequest("dup@example.com");

        assertThatThrownBy(() -> personService.createPerson(req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(personRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldReturnResponse_whenPersonExists() {
        Person person = buildPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personMapper.toResponse(person)).thenReturn(buildPersonResponse());

        PersonResponse result = personService.findById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void findById_shouldThrow404_whenPersonNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.findById(99L))
                .isInstanceOf(PersonNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    void existsById_shouldReturnTrue_whenPersonExists() {
        when(personRepository.existsById(1L)).thenReturn(true);
        assertThat(personService.existsById(1L)).isTrue();
    }

    @Test
    void existsById_shouldReturnFalse_whenPersonNotFound() {
        when(personRepository.existsById(99L)).thenReturn(false);
        assertThat(personService.existsById(99L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // updatePerson
    // -------------------------------------------------------------------------

    @Test
    void updatePerson_shouldUpdate_whenEmailUnchanged() {
        Person person = buildPerson();
        person.setEmail("john@example.com");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personRepository.save(any())).thenReturn(person);
        when(personMapper.toResponse(any())).thenReturn(buildPersonResponse());

        UpdatePersonRequest req = new UpdatePersonRequest();
        req.setFirstName("Jane");

        personService.updatePerson(1L, req);

        assertThat(person.getFirstName()).isEqualTo("Jane");
        verify(auditService).log(any());
    }

    @Test
    void updatePerson_shouldThrow409_whenNewEmailAlreadyTaken() {
        Person person = buildPerson();
        person.setEmail("old@example.com");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personRepository.existsByEmailExcludingId("new@example.com", 1L)).thenReturn(true);

        UpdatePersonRequest req = new UpdatePersonRequest();
        req.setEmail("new@example.com");

        assertThatThrownBy(() -> personService.updatePerson(1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // deletePerson
    // -------------------------------------------------------------------------

    @Test
    void deletePerson_shouldSoftDelete_whenNoLinkedUser() {
        Person person = buildPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(userRepository.findByPersonId(1L)).thenReturn(Optional.empty());
        when(personRepository.save(any())).thenReturn(person);

        personService.deletePerson(1L);

        assertThat(person.isDeleted()).isTrue();
        assertThat(person.getDeletedAt()).isNotNull();
        verify(auditService).log(any());
    }

    @Test
    void deletePerson_shouldThrow409_whenActiveUserExists() {
        Person person = buildPerson();
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(userRepository.findByPersonId(1L)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> personService.deletePerson(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("active user account");
    }

    @Test
    void deletePerson_shouldThrow404_whenPersonNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.deletePerson(99L))
                .isInstanceOf(PersonNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Person buildPerson() {
        return Person.builder()
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private PersonResponse buildPersonResponse() {
        return PersonResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .build();
    }

    private CreatePersonRequest buildCreateRequest(String email) {
        CreatePersonRequest req = new CreatePersonRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail(email);
        req.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return req;
    }
}
