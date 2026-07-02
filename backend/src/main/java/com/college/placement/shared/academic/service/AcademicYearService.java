package com.college.placement.shared.academic.service;

import com.college.placement.shared.academic.domain.AcademicYear;
import com.college.placement.shared.academic.repository.AcademicYearRepository;
import com.college.placement.shared.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages academic years and the single-active-year invariant. Mutations are
 * audited; activation atomically deactivates the previously active year.
 */
@Service
@Transactional
public class AcademicYearService {

    private static final String ENTITY = "AcademicYear";

    private final AcademicYearRepository repository;
    private final AuditService auditService;

    public AcademicYearService(AcademicYearRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public AcademicYear create(String label, LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "End date must be after start date");
        }
        if (repository.existsByLabel(label)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Academic year already exists: " + label);
        }
        AcademicYear year = new AcademicYear();
        year.setLabel(label);
        year.setStartDate(startDate);
        year.setEndDate(endDate);
        year.setActive(false);
        AcademicYear saved = repository.save(year);
        auditService.record(ENTITY, saved.getId(), "ACADEMIC_YEAR_CREATED");
        return saved;
    }

    public AcademicYear update(UUID id, LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "End date must be after start date");
        }
        AcademicYear year = getById(id);
        year.setStartDate(startDate);
        year.setEndDate(endDate);
        AcademicYear saved = repository.save(year);
        auditService.record(ENTITY, saved.getId(), "ACADEMIC_YEAR_UPDATED");
        return saved;
    }

    public AcademicYear activate(UUID id) {
        AcademicYear target = getById(id);
        repository.findByActiveTrue().ifPresent(current -> {
            if (!current.getId().equals(target.getId())) {
                current.setActive(false);
                repository.save(current);
            }
        });
        target.setActive(true);
        AcademicYear saved = repository.save(target);
        auditService.record(ENTITY, saved.getId(), "ACADEMIC_YEAR_ACTIVATED");
        return saved;
    }

    @Transactional(readOnly = true)
    public AcademicYear getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));
    }

    @Transactional(readOnly = true)
    public Page<AcademicYear> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<AcademicYear> findActive() {
        return repository.findByActiveTrue();
    }

    /** The currently active academic year, or a 409 if none has been activated. */
    @Transactional(readOnly = true)
    public AcademicYear requireActive() {
        return repository.findByActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No active academic year configured"));
    }
}
