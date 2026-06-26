package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.domain.Branch;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Branch response")
public record BranchResponse(
        UUID id,
        String name,
        String code,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static BranchResponse from(Branch b) {
        return new BranchResponse(b.getId(), b.getName(), b.getCode(),
                b.getDescription(), b.isActive(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
