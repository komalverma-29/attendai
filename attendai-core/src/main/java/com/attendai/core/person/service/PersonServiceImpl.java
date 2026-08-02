package com.attendai.core.person.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.dto.CreatePersonRequest;
import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.dto.PersonSummaryResponse;
import com.attendai.core.person.dto.UpdatePersonRequest;
import com.attendai.core.person.entity.Person;
import com.attendai.core.person.exception.PersonNotFoundException;
import com.attendai.core.person.mapper.PersonMapper;
import com.attendai.core.person.repository.PersonRepository;
import com.attendai.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link PersonService}.
 *
 * Security notes:
 * - PII fields (email, phone, identityDocNumber) are NEVER included in log messages.
 * - Audit events capture only person_id — not PII content.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final UserRepository   userRepository;
    private final PersonMapper     personMapper;
    private final AuditService     auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public PersonResponse createPerson(CreatePersonRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalised = request.getEmail().trim().toLowerCase();
            if (personRepository.existsByEmailIncludingDeleted(normalised)) {
                throw new ResourceAlreadyExistsException(
                        "Person with email '" + normalised + "' already exists");
            }
            request.setEmail(normalised);
        }

        Person person = Person.builder()
                .firstName(request.getFirstName().trim())
                .middleName(trim(request.getMiddleName()))
                .lastName(request.getLastName().trim())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .phone(trim(request.getPhone()))
                .addressLine1(trim(request.getAddressLine1()))
                .addressLine2(trim(request.getAddressLine2()))
                .city(trim(request.getCity()))
                .stateOrProvince(trim(request.getStateOrProvince()))
                .postalCode(trim(request.getPostalCode()))
                .country(request.getCountry() != null ? request.getCountry().toUpperCase() : null)
                .identityDocType(request.getIdentityDocType())
                .identityDocNumber(trim(request.getIdentityDocNumber()))
                .profilePhotoFileId(request.getProfilePhotoFileId())
                .build();

        Person saved = personRepository.save(person);
        log.info("Person created | personId={}", saved.getId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("PERSON_CREATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Person")
                .resourceId(String.valueOf(saved.getId()))
                .build());

        return personMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PersonResponse findById(Long id) {
        return personMapper.toResponse(requirePerson(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse findByIdOrThrow(Long id) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return personRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonSummaryResponse> searchPersons(String search, Pageable pageable) {
        String normalised = (search != null && !search.isBlank()) ? search.trim() : null;
        return personRepository.search(normalised, pageable).map(personMapper::toSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public PersonResponse updatePerson(Long id, UpdatePersonRequest request) {
        Person person = requirePerson(id);

        // Email uniqueness check — only if email is being changed
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(person.getEmail()) &&
                    personRepository.existsByEmailExcludingId(newEmail, id)) {
                throw new ResourceAlreadyExistsException(
                        "Person with email '" + newEmail + "' already exists");
            }
            person.setEmail(newEmail.isBlank() ? null : newEmail);
        }

        // Apply all non-null fields from the request
        if (request.getFirstName()     != null) person.setFirstName(request.getFirstName().trim());
        if (request.getMiddleName()    != null) person.setMiddleName(request.getMiddleName().trim());
        if (request.getLastName()      != null) person.setLastName(request.getLastName().trim());
        if (request.getGender()        != null) person.setGender(request.getGender());
        if (request.getDateOfBirth()   != null) person.setDateOfBirth(request.getDateOfBirth());
        if (request.getPhone()         != null) person.setPhone(trim(request.getPhone()));
        if (request.getAddressLine1()  != null) person.setAddressLine1(trim(request.getAddressLine1()));
        if (request.getAddressLine2()  != null) person.setAddressLine2(trim(request.getAddressLine2()));
        if (request.getCity()          != null) person.setCity(trim(request.getCity()));
        if (request.getStateOrProvince() != null) person.setStateOrProvince(trim(request.getStateOrProvince()));
        if (request.getPostalCode()    != null) person.setPostalCode(trim(request.getPostalCode()));
        if (request.getCountry()       != null) person.setCountry(request.getCountry().toUpperCase());
        if (request.getIdentityDocType() != null) person.setIdentityDocType(request.getIdentityDocType());
        if (request.getIdentityDocNumber() != null) person.setIdentityDocNumber(trim(request.getIdentityDocNumber()));
        if (request.getProfilePhotoFileId() != null) person.setProfilePhotoFileId(request.getProfilePhotoFileId());

        Person saved = personRepository.save(person);
        log.info("Person updated | personId={}", saved.getId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("PERSON_UPDATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Person")
                .resourceId(String.valueOf(id))
                .build());

        return personMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deletePerson(Long id) {
        Person person = requirePerson(id);

        // Guard: person cannot be deleted if they have an active user account
        if (userRepository.findByPersonId(id).isPresent()) {
            throw new ValidationException(
                    "Person with id " + id + " has an active user account and cannot be deleted");
        }

        // Guard: face profile check deferred — core-face module not yet implemented.
        // When core-face is implemented, add:
        // if (faceProfileRepository.existsByPersonIdAndIsDeletedFalse(id)) { throw ... }

        person.softDelete();
        personRepository.save(person);
        log.info("Person soft-deleted | personId={}", id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("PERSON_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Person")
                .resourceId(String.valueOf(id))
                .build());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Person requirePerson(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }

    /** Trims a nullable string; returns null if blank after trimming. */
    private static String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
