package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Student profile response")
public record StudentResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String rollNumber,
        UUID branchId,
        String branchName,
        BigDecimal cgpa,
        int currentYear,
        boolean placementEligible,
        StudentStatus status,
        Set<String> skillNames,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudentResponse from(Student s) {
        return new StudentResponse(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getEmail(),
                s.getRollNumber(),
                s.getBranch() != null ? s.getBranch().getId() : null,
                s.getBranch() != null ? s.getBranch().getName() : null,
                s.getCgpa(),
                s.getCurrentYear(),
                s.isPlacementEligible(),
                s.getStatus(),
                s.getSkills().stream().map(sk -> sk.getName()).collect(Collectors.toSet()),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
