package com.college.placement.modules.placement.dto;

import com.college.placement.modules.placement.domain.Offer;
import com.college.placement.modules.placement.domain.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Placement offer response")
public record OfferResponse(
        UUID id,
        UUID applicationId,
        UUID studentId,
        String studentRollNumber,
        UUID companyId,
        String companyName,
        OfferStatus status,
        BigDecimal ctc,
        LocalDate joiningDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static OfferResponse from(Offer o) {
        return new OfferResponse(
                o.getId(),
                o.getApplication().getId(),
                o.getApplication().getStudent().getId(),
                o.getApplication().getStudent().getRollNumber(),
                o.getApplication().getJobPosting().getCompany().getId(),
                o.getApplication().getJobPosting().getCompany().getName(),
                o.getStatus(),
                o.getCtc(),
                o.getJoiningDate(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
