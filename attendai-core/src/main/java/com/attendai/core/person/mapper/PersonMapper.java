package com.attendai.core.person.mapper;

import com.attendai.core.common.util.StringUtils;
import com.attendai.core.person.dto.PersonResponse;
import com.attendai.core.person.dto.PersonSummaryResponse;
import com.attendai.core.person.entity.Person;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for Person entities.
 *
 * The {@code fullName} field is a computed value assembled in the
 * {@link #setFullName} after-mapping method using
 * {@link StringUtils#buildFullName(String...)}.
 */
@Mapper(componentModel = "spring")
public interface PersonMapper {

    /** Maps Person → PersonResponse. fullName is populated by @AfterMapping. */
    @Mapping(target = "fullName", ignore = true)
    PersonResponse toResponse(Person person);

    /** Maps Person → PersonSummaryResponse. fullName is populated by @AfterMapping. */
    @Mapping(target = "fullName", ignore = true)
    PersonSummaryResponse toSummaryResponse(Person person);

    @AfterMapping
    default void setFullName(Person person, @MappingTarget PersonResponse.PersonResponseBuilder builder) {
        builder.fullName(StringUtils.buildFullName(
                person.getFirstName(), person.getMiddleName(), person.getLastName()));
    }

    @AfterMapping
    default void setSummaryFullName(Person person,
                                     @MappingTarget PersonSummaryResponse.PersonSummaryResponseBuilder builder) {
        builder.fullName(StringUtils.buildFullName(
                person.getFirstName(), person.getMiddleName(), person.getLastName()));
    }
}
