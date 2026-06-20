package com.college.placement.modules.aigovernance.repository;

import com.college.placement.modules.aigovernance.domain.AIModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AIModelRepository extends JpaRepository<AIModel, UUID> {

    Optional<AIModel> findByName(String name);

    boolean existsByName(String name);

    List<AIModel> findByEnabledTrue();
}
