package com.attendai.core.user.mapper;

import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.dto.UserSummaryResponse;
import com.attendai.core.user.entity.User;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between {@link User} entity and response DTOs.
 *
 * The {@code passwordHash} field is intentionally absent from both DTOs —
 * MapStruct will never include it in any mapping.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    UserSummaryResponse toSummaryResponse(User user);
}
