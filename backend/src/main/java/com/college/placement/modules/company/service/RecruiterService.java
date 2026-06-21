package com.college.placement.modules.company.service;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.Recruiter;
import com.college.placement.modules.company.event.RecruiterRegisteredEvent;
import com.college.placement.modules.company.repository.RecruiterRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final CompanyService companyService;
    private final EventPublisher eventPublisher;

    public RecruiterService(RecruiterRepository recruiterRepository,
                            CompanyService companyService,
                            EventPublisher eventPublisher) {
        this.recruiterRepository = recruiterRepository;
        this.companyService = companyService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Recruiter registerRecruiter(AppUser user, UUID companyId, String designation) {
        if (recruiterRepository.existsByUser(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recruiter profile already exists for this user");
        }
        Company company = companyService.getById(companyId);
        Recruiter recruiter = new Recruiter();
        recruiter.setUser(user);
        recruiter.setCompany(company);
        recruiter.setDesignation(designation);
        Recruiter saved = recruiterRepository.save(recruiter);
        eventPublisher.publish(RecruiterRegisteredEvent.of(
                saved.getId(), user.getId(), company.getId(), company.getName()));
        return saved;
    }

    @Transactional
    public Recruiter updateDesignation(UUID recruiterId, String designation) {
        Recruiter recruiter = getById(recruiterId);
        recruiter.setDesignation(designation);
        return recruiterRepository.save(recruiter);
    }

    @Transactional(readOnly = true)
    public Recruiter getById(UUID id) {
        return recruiterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found"));
    }

    @Transactional(readOnly = true)
    public Recruiter getByUser(AppUser user) {
        return recruiterRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter profile not found"));
    }

    @Transactional(readOnly = true)
    public List<Recruiter> getByCompany(UUID companyId) {
        Company company = companyService.getById(companyId);
        return recruiterRepository.findByCompany(company);
    }
}
