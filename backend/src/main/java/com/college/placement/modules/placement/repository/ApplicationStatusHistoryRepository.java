package com.college.placement.modules.placement.repository;

import com.college.placement.modules.placement.domain.ApplicationStatusHistory;
import com.college.placement.modules.placement.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplicationOrderByCreatedAtAsc(JobApplication application);
}
