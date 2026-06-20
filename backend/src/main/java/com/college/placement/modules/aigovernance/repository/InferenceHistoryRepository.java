package com.college.placement.modules.aigovernance.repository;

import com.college.placement.modules.aigovernance.domain.AIModel;
import com.college.placement.modules.aigovernance.domain.InferenceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InferenceHistoryRepository extends JpaRepository<InferenceHistory, UUID> {

    Page<InferenceHistory> findByAiModel(AIModel aiModel, Pageable pageable);

    Page<InferenceHistory> findByRequestedBy(String requestedBy, Pageable pageable);

    Page<InferenceHistory> findBySuccessFalse(Pageable pageable);
}
