package com.college.placement.modules.placement.repository;

import com.college.placement.modules.placement.domain.JobApplication;
import com.college.placement.modules.placement.domain.Offer;
import com.college.placement.modules.placement.domain.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByApplication(JobApplication application);

    boolean existsByApplication(JobApplication application);

    List<Offer> findByStatus(OfferStatus status);
}
