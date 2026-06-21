package com.college.placement.modules.placement.repository;

import com.college.placement.modules.placement.domain.JobApplication;
import com.college.placement.modules.placement.domain.Offer;
import com.college.placement.modules.placement.domain.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @EntityGraph(attributePaths = {"application", "application.student", "application.jobPosting", "application.jobPosting.company"})
    Optional<Offer> findById(UUID id);

    @EntityGraph(attributePaths = {"application", "application.student", "application.jobPosting", "application.jobPosting.company"})
    Page<Offer> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"application", "application.student", "application.jobPosting", "application.jobPosting.company"})
    Optional<Offer> findByApplication(JobApplication application);

    boolean existsByApplication(JobApplication application);

    List<Offer> findByStatus(OfferStatus status);
}
