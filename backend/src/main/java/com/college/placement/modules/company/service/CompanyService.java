package com.college.placement.modules.company.service;

import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.CompanyStatus;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.CompanyCreatedEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final EventPublisher eventPublisher;

    public CompanyService(CompanyRepository companyRepository, EventPublisher eventPublisher) {
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Company createCompany(String name, String website, String industry, String description) {
        if (companyRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Company name already registered");
        }
        Company company = new Company();
        company.setName(name);
        company.setWebsite(website);
        company.setIndustry(industry);
        company.setDescription(description);
        Company saved = companyRepository.save(company);
        eventPublisher.publish(CompanyCreatedEvent.of(saved.getId(), name, industry));
        return saved;
    }

    @Transactional
    public Company updateCompany(UUID id, String name, String website, String industry, String description, String logoUrl) {
        Company company = getById(id);
        if (!company.getName().equals(name) && companyRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Company name already registered");
        }
        company.setName(name);
        company.setWebsite(website);
        company.setIndustry(industry);
        company.setDescription(description);
        company.setLogoUrl(logoUrl);
        return companyRepository.save(company);
    }

    @Transactional
    public Company activate(UUID id) {
        Company company = getById(id);
        if (company.getStatus() == CompanyStatus.BLACKLISTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Blacklisted companies cannot be reactivated");
        }
        company.setStatus(CompanyStatus.ACTIVE);
        return companyRepository.save(company);
    }

    @Transactional
    public Company deactivate(UUID id) {
        Company company = getById(id);
        company.setStatus(CompanyStatus.INACTIVE);
        return companyRepository.save(company);
    }

    @Transactional
    public Company blacklist(UUID id) {
        Company company = getById(id);
        company.setStatus(CompanyStatus.BLACKLISTED);
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    @Transactional(readOnly = true)
    public List<Company> getAll() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Company> getByIndustry(String industry) {
        return companyRepository.findByIndustry(industry);
    }

    @Transactional(readOnly = true)
    public Page<Company> getAll(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }
}
