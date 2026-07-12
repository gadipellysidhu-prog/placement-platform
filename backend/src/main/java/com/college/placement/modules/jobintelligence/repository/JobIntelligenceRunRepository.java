package com.college.placement.modules.jobintelligence.repository;

import com.college.placement.modules.jobintelligence.domain.JobIntelligenceRun;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobIntelligenceRunRepository extends JpaRepository<JobIntelligenceRun, UUID> {

    Optional<JobIntelligenceRun> findTopByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);

    List<JobIntelligenceRun> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);

    boolean existsByJobPostingIdAndStatusIn(UUID jobPostingId, List<RunStatus> statuses);

    /** Stuck runs (crash recovery): still PENDING after the given cutoff. */
    List<JobIntelligenceRun> findByStatusAndCreatedAtBefore(RunStatus status, Instant cutoff);
}
