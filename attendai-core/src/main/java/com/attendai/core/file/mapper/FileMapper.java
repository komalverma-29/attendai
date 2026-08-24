package com.attendai.core.file.mapper;

import com.attendai.core.file.dto.FileMetadataResponse;
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.entity.FileRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for FileRecord entities.
 * {@code storageKey} is intentionally absent from both DTOs and is never mapped.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper {
    FileUploadResponse   toUploadResponse(FileRecord fileRecord);
    FileMetadataResponse toMetadataResponse(FileRecord fileRecord);
}
