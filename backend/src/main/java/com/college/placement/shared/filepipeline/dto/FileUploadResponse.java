package com.college.placement.shared.filepipeline.dto;

import com.college.placement.shared.filepipeline.domain.FileScanRecord;
import com.college.placement.shared.filepipeline.domain.FileScanStatus;

import java.time.Instant;
import java.util.UUID;

public record FileUploadResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        String sha256Hash,
        FileScanStatus scanStatus,
        boolean quarantined,
        String uploadedBy,
        Instant uploadedAt
) {
    public static FileUploadResponse from(FileScanRecord record) {
        return new FileUploadResponse(
                record.getId(),
                record.getFilename(),
                record.getContentType(),
                record.getSizeBytes() != null ? record.getSizeBytes() : 0L,
                record.getSha256Hash(),
                record.getScanStatus(),
                record.isQuarantined(),
                record.getUploadedBy(),
                record.getCreatedAt()
        );
    }
}
