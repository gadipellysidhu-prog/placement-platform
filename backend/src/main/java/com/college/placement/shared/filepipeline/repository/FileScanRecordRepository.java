package com.college.placement.shared.filepipeline.repository;

import com.college.placement.shared.filepipeline.domain.FileScanRecord;
import com.college.placement.shared.filepipeline.domain.FileScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileScanRecordRepository extends JpaRepository<FileScanRecord, UUID> {

    Optional<FileScanRecord> findByStorageKey(String storageKey);

    Optional<FileScanRecord> findBySha256Hash(String sha256Hash);

    List<FileScanRecord> findByScanStatus(FileScanStatus scanStatus);
}
