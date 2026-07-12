package com.college.placement.modules.jobintelligence.repository;

import com.college.placement.modules.jobintelligence.domain.ExtractionCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ExtractionCacheRepository extends JpaRepository<ExtractionCacheEntry, UUID> {

    Optional<ExtractionCacheEntry> findByUrlHash(String urlHash);

    void deleteByUrlHash(String urlHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ExtractionCacheEntry e where e.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
