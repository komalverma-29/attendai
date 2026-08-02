package com.attendai.core.person.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.person.dto.CreatePersonRequest;
import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.exception.PersonNotFoundException;
import com.attendai.core.person.service.PersonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonController.class)
@Import(SecurityConfig.class)
class PersonControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PersonService personService;

    // MockBeans required by SecurityConfig
    @MockBean com.attendai.core.auth.service.JwtService         jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;

    private static final String BASE = "/api/v1/core/persons";

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_PERSON_CREATE")
    void createPerson_shouldReturn201_whenValidRequest() throws Exception {
        PersonResponse resp = PersonResponse.builder()
                .id(1L).firstName("John").lastName("Doe").fullName("John Doe").build();
        when(personService.createPerson(any())).thenReturn(resp);

        CreatePersonRequest req = new CreatePersonRequest();
        req.setFirstName("John");
        req.setLastName("Doe");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(authorities = "CORE_PERSON_CREATE")
    void createPerson_shouldReturn400_whenFirstNameMissing() throws Exception {
        CreatePersonRequest req = new CreatePersonRequest();
        req.setLastName("Doe"); // firstName missing

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("firstName"));
    }

    @Test
    void createPerson_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CORE_PERSON_READ")
    void createPerson_shouldReturn403_whenMissingCreatePermission() throws Exception {
        CreatePersonRequest req = new CreatePersonRequest();
        req.setFirstName("John");
        req.setLastName("Doe");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET by id
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_PERSON_READ")
    void getPerson_shouldReturn200_whenFound() throws Exception {
        PersonResponse resp = PersonResponse.builder()
                .id(1L).firstName("John").lastName("Doe").fullName("John Doe").build();
        when(personService.findById(1L)).thenReturn(resp);

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(authorities = "CORE_PERSON_READ")
    void getPerson_shouldReturn404_whenNotFound() throws Exception {
        when(personService.findById(99L)).thenThrow(new PersonNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_PERSON_DELETE")
    void deletePerson_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CORE_PERSON_DELETE")
    void deletePerson_shouldReturn400_whenPersonHasActiveUser() throws Exception {
        doThrow(new com.attendai.core.common.exception.ValidationException(
                "Person has an active user account"))
                .when(personService).deletePerson(1L);

        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "CORE_PERSON_READ")
    void deletePerson_shouldReturn403_whenMissingDeletePermission() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isForbidden());
    }
}
