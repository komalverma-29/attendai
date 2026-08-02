package com.attendai.core.person.mapper;

import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.dto.PersonSummaryResponse;
import com.attendai.core.person.entity.Gender;
import com.attendai.core.person.entity.Person;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the PersonMapper, specifically the computed fullName field
 * assembled by the @AfterMapping method.
 */
class PersonMapperTest {

    private final PersonMapper mapper = Mappers.getMapper(PersonMapper.class);

    @Test
    void toResponse_shouldAssembleFullName_withAllThreeParts() {
        Person person = Person.builder()
                .firstName("John")
                .middleName("Michael")
                .lastName("Doe")
                .gender(Gender.MALE)
                .build();

        PersonResponse response = mapper.toResponse(person);

        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getFullName()).isEqualTo("John Michael Doe");
    }

    @Test
    void toResponse_shouldAssembleFullName_withoutMiddleName() {
        Person person = Person.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        PersonResponse response = mapper.toResponse(person);

        assertThat(response.getFullName()).isEqualTo("Jane Smith");
    }

    @Test
    void toResponse_shouldHandleNullMiddleName() {
        Person person = Person.builder()
                .firstName("Alice")
                .middleName(null)
                .lastName("Wong")
                .build();

        PersonResponse response = mapper.toResponse(person);

        assertThat(response.getFullName()).isEqualTo("Alice Wong");
    }

    @Test
    void toSummaryResponse_shouldAssembleFullName() {
        Person person = Person.builder()
                .firstName("Bob")
                .lastName("Brown")
                .email("bob@example.com")
                .phone("+1-555-0100")
                .build();

        PersonSummaryResponse summary = mapper.toSummaryResponse(person);

        assertThat(summary.getFullName()).isEqualTo("Bob Brown");
        assertThat(summary.getEmail()).isEqualTo("bob@example.com");
        assertThat(summary.getPhone()).isEqualTo("+1-555-0100");
    }
}
