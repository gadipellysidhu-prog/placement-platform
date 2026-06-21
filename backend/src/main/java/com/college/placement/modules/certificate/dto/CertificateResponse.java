package com.college.placement.modules.certificate.dto;

import com.college.placement.modules.certificate.domain.Certificate;
import com.college.placement.modules.certificate.domain.CertificateVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Certificate response")
public record CertificateResponse(
        UUID id,
        UUID studentId,
        String studentRollNumber,
        UUID skillId,
        String skillName,
        String name,
        String issuingOrganization,
        String fileKey,
        CertificateVerificationStatus verificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static CertificateResponse from(Certificate c) {
        return new CertificateResponse(
                c.getId(),
                c.getStudent().getId(),
                c.getStudent().getRollNumber(),
                c.getSkill() != null ? c.getSkill().getId() : null,
                c.getSkill() != null ? c.getSkill().getName() : null,
                c.getName(),
                c.getIssuingOrganization(),
                c.getFileKey(),
                c.getVerificationStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
