package com.attendai.core.face.mapper;

import com.attendai.core.face.dto.FaceImageResponse;
import com.attendai.core.face.dto.FaceProfileResponse;
import com.attendai.core.face.entity.FaceImage;
import com.attendai.core.face.entity.FaceProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for core-face entities.
 *
 * {@code embeddingVector} is intentionally absent from {@link FaceImageResponse}
 * and will never be mapped — it must not be exposed through any API response.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FaceMapper {
    FaceProfileResponse toProfileResponse(FaceProfile profile);
    FaceImageResponse   toImageResponse(FaceImage image);
}
