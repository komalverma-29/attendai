package com.attendai.core.file.repository;

import com.attendai.core.file.entity.FileRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    /** Lists files uploaded by a specific user, optionally filtered by module. */
    @Query("""
            SELECT f FROM FileRecord f
            WHERE f.uploadedByUserId = :userId
              AND (:module IS NULL OR f.module = :module)
            ORDER BY f.createdAt DESC
            """)
    Page<FileRecord> findByUploadedByUserIdAndModule(
            @Param("userId") Long userId,
            @Param("module") String module,
            Pageable pageable);
}
