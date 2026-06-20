package com.college.placement.modules.student.repository;

import com.college.placement.modules.student.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByName(String name);

    Optional<Branch> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    List<Branch> findByActiveTrue();
}
