package com.college.placement.modules.student.service;

import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.repository.BranchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Transactional
    public Branch createBranch(String name, String code, String description) {
        if (branchRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch name already exists");
        }
        if (code != null && branchRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch code already exists");
        }
        Branch branch = new Branch();
        branch.setName(name);
        branch.setCode(code);
        branch.setDescription(description);
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(UUID id, String name, String code, String description) {
        Branch branch = getById(id);
        if (!branch.getName().equals(name) && branchRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch name already exists");
        }
        branch.setName(name);
        branch.setCode(code);
        branch.setDescription(description);
        return branchRepository.save(branch);
    }

    @Transactional
    public void deactivate(UUID id) {
        Branch branch = getById(id);
        branch.setActive(false);
        branchRepository.save(branch);
    }

    @Transactional
    public void activate(UUID id) {
        Branch branch = getById(id);
        branch.setActive(true);
        branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public Branch getById(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    @Transactional(readOnly = true)
    public List<Branch> getAll() {
        return branchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Branch> getActiveBranches() {
        return branchRepository.findByActiveTrue();
    }
}
