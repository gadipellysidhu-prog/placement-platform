package com.college.placement.modules.certificate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to submit a certificate for verification")
public record CertificateCreateRequest(

        @NotNull
        @Schema(description = "Student UUID")
        UUID studentId,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Certificate name", example = "AWS Certified Developer")
        String name,

        @Size(max = 255)
        @Schema(description = "Issuing organization", example = "Amazon Web Services")
        String issuingOrganization,

        @Schema(description = "Related skill UUID (optional)")
        UUID skillId,

        @Size(max = 500)
        @Schema(description = "Storage file key for the certificate document")
        String fileKey
) {}
