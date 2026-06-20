package com.college.placement.modules.aigovernance.repository;

import com.college.placement.modules.aigovernance.domain.AIModel;
import com.college.placement.modules.aigovernance.domain.PromptRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptRegistryRepository extends JpaRepository<PromptRegistry, UUID> {

    Optional<PromptRegistry> findByPromptKey(String promptKey);

    boolean existsByPromptKey(String promptKey);

    List<PromptRegistry> findByAiModel(AIModel aiModel);
}
