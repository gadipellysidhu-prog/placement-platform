package com.college.placement.shared.filepipeline.service;

import com.college.placement.shared.filepipeline.domain.FileScanRecord;
import com.college.placement.shared.filepipeline.domain.FileScanStatus;
import com.college.placement.shared.filepipeline.dto.FileUploadResponse;
import com.college.placement.shared.filepipeline.exception.FileStorageException;
import com.college.placement.shared.filepipeline.exception.VirusDetectedException;
import com.college.placement.shared.filepipeline.repository.FileScanRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class FilePipelineService {

    private final FileValidationService validationService;
    private final FileHashService hashService;
    private final FileStorageService storageService;
    private final ClamAvService clamAvService;
    private final FileScanRecordRepository recordRepository;

    public FilePipelineService(FileValidationService validationService,
                               FileHashService hashService,
                               FileStorageService storageService,
                               ClamAvService clamAvService,
                               FileScanRecordRepository recordRepository) {
        this.validationService = validationService;
        this.hashService       = hashService;
        this.storageService    = storageService;
        this.clamAvService     = clamAvService;
        this.recordRepository  = recordRepository;
    }

    /**
     * Full upload pipeline: validate → hash → store → persist → scan → update.
     * <p>
     * Each repository write is its own short-lived transaction so the DB connection
     * is not held open during the ClamAV I/O.
     */
    public FileUploadResponse upload(MultipartFile file, String uploadedBy) {
        // 1. Validate MIME type, size, and magic bytes — reject early if invalid.
        validationService.validate(file);

        // 2. Hash the raw bytes before storage for deduplication support.
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read uploaded file", ex);
        }
        String sha256 = hashService.sha256Hex(fileBytes);

        // 3. Write to disk; obtain unique storage key.
        String storageKey = storageService.store(file);

        // 4. Persist a PENDING record so the upload is traceable even if scanning fails.
        FileScanRecord record = new FileScanRecord();
        record.setFilename(sanitizeFilename(file.getOriginalFilename()));
        record.setStorageKey(storageKey);
        record.setContentType(file.getContentType());
        record.setSizeBytes(file.getSize());
        record.setSha256Hash(sha256);
        record.setScanStatus(FileScanStatus.PENDING);
        record.setUploadedBy(uploadedBy);
        record = recordRepository.save(record);

        log.info("File uploaded: id={} filename={} uploadedBy={}", record.getId(), record.getFilename(), uploadedBy);

        // 5. Virus scan (I/O outside any transaction).
        FileScanStatus scanStatus;
        try (InputStream in = storageService.load(storageKey).getInputStream()) {
            scanStatus = clamAvService.scan(in);
        } catch (IOException ex) {
            log.error("Could not open stored file for scanning: storageKey={}", storageKey, ex);
            scanStatus = FileScanStatus.FAILED;
        }

        // 6. Update scan outcome.
        record.setScanStatus(scanStatus);
        if (scanStatus == FileScanStatus.INFECTED) {
            record.setQuarantined(true);
            record = recordRepository.save(record);
            storageService.delete(storageKey); // remove infected file
            log.warn("Infected file quarantined and deleted: id={}", record.getId());
            throw new VirusDetectedException(record.getFilename());
        }

        record = recordRepository.save(record);
        log.info("File scan complete: id={} status={}", record.getId(), scanStatus);

        return FileUploadResponse.from(record);
    }

    /**
     * Streams the file with the given record ID.
     * Quarantined files are blocked regardless of their scan status.
     */
    public Resource download(UUID id) {
        FileScanRecord record = findOrThrow(id);
        if (record.isQuarantined()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "File is quarantined and cannot be downloaded.");
        }
        return storageService.load(record.getStorageKey());
    }

    /**
     * Returns the metadata record for response-header construction (content-type, filename).
     */
    public FileScanRecord findRecord(UUID id) {
        return findOrThrow(id);
    }

    /**
     * Removes the physical file and its metadata record.
     */
    public void delete(UUID id) {
        FileScanRecord record = findOrThrow(id);
        String storageKey = record.getStorageKey();

        // Delete DB record first; if disk delete fails, the record is gone and the
        // orphaned file will be cleaned up by a future maintenance job.
        recordRepository.delete(record);
        storageService.delete(storageKey);

        log.info("File deleted by operator: id={} filename={}", id, record.getFilename());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private FileScanRecord findOrThrow(UUID id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "File record not found: " + id));
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }
        // Strip any path component the client may have included.
        String basename = Path.of(original).getFileName().toString();
        // Allow only safe characters in the stored filename.
        return basename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
