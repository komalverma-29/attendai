package com.attendai.core.person.service;

import com.attendai.core.person.dto.CreatePersonRequest;
import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.dto.PersonSummaryResponse;
import com.attendai.core.person.dto.UpdatePersonRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Core person service.
 *
 * Manages person lifecycle: create, read, search, update, and soft-delete.
 * Exposes a lightweight internal API consumed by core-user, core-face,
 * core-attendance, and all business modules.
 */
public interface PersonService {

    // -------------------------------------------------------------------------
    // HTTP-facing operations
    // -------------------------------------------------------------------------

    PersonResponse createPerson(CreatePersonRequest request);

    PersonResponse findById(Long id);

    Page<PersonSummaryResponse> searchPersons(String search, Pageable pageable);

    PersonResponse updatePerson(Long id, UpdatePersonRequest request);

    void deletePerson(Long id);

    // -------------------------------------------------------------------------
    // Internal API (consumed by other Core modules and business modules)
    // -------------------------------------------------------------------------

    /**
     * Returns true when the person exists and is NOT soft-deleted.
     * Used by core-user and core-face before creating linked records.
     */
    boolean existsById(Long id);

    /**
     * Retrieves a person by ID or throws {@link com.attendai.core.person.exception.PersonNotFoundException}.
     * Used by business modules to resolve person details.
     */
    PersonResponse findByIdOrThrow(Long id);
}
