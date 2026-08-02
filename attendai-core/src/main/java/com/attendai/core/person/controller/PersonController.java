package com.attendai.core.person.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.person.dto.CreatePersonRequest;
import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.dto.PersonSummaryResponse;
import com.attendai.core.person.dto.UpdatePersonRequest;
import com.attendai.core.person.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for person management.
 *
 * Base path: /api/v1/core/persons
 *
 * No business logic lives here — all work delegates to {@link PersonService}.
 */
@RestController
@RequestMapping("/api/v1/core/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    /** POST /api/v1/core/persons — Create a new person. */
    @PostMapping
    @PreAuthorize("hasAuthority('CORE_PERSON_CREATE')")
    public ResponseEntity<ApiResponse<PersonResponse>> createPerson(
            @Valid @RequestBody CreatePersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(personService.createPerson(request)));
    }

    /** GET /api/v1/core/persons/{id} — Retrieve a person by ID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_PERSON_READ')")
    public ResponseEntity<ApiResponse<PersonResponse>> getPerson(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(personService.findById(id)));
    }

    /**
     * GET /api/v1/core/persons — Search/list persons.
     * Optional {@code search} param matches first name, last name, email, or phone.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CORE_PERSON_READ')")
    public ResponseEntity<PageResponse<PersonSummaryResponse>> searchPersons(
            @RequestParam(required = false) String search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(
                PageResponse.of(personService.searchPersons(search, pageParams.toPageable())));
    }

    /** PUT /api/v1/core/persons/{id} — Update a person. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_PERSON_UPDATE')")
    public ResponseEntity<ApiResponse<PersonResponse>> updatePerson(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePersonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(personService.updatePerson(id, request)));
    }

    /** DELETE /api/v1/core/persons/{id} — Soft-delete a person. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_PERSON_DELETE')")
    public ResponseEntity<Void> deletePerson(@PathVariable("id") Long id) {
        personService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }
}
