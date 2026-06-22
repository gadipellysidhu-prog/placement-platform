package com.college.placement.shared.filepipeline.service;

import com.college.placement.shared.filepipeline.config.FilePipelineProperties;
import com.college.placement.shared.filepipeline.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final FilePipelineProperties props;
    private Path uploadDir;

    public FileStorageService(FilePipelineProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Path.of(props.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        log.info("File storage directory: {}", uploadDir);
    }

    /**
     * Persists the multipart file under a UUID-based storage key.
     *
     * @return the storage key (UUID string) that uniquely identifies the file on disk
     */
    public String store(MultipartFile file) {
        String storageKey = UUID.randomUUID().toString();
        Path target = resolveAndGuard(storageKey);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file: storageKey={} size={}", storageKey, file.getSize());
            return storageKey;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to persist file to disk", ex);
        }
    }

    /**
     * Resolves a storage key to a Spring {@link Resource} suitable for streaming.
     */
    public Resource load(String storageKey) {
        Path target = resolveAndGuard(storageKey);
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "File not found for storage key: " + storageKey);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Invalid storage key: " + storageKey, ex);
        }
    }

    /**
     * Removes the physical file associated with the given storage key.
     * Silently succeeds if the file is already absent.
     */
    public void delete(String storageKey) {
        Path target = resolveAndGuard(storageKey);
        try {
            boolean deleted = Files.deleteIfExists(target);
            log.debug("Deleted file: storageKey={} existed={}", storageKey, deleted);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete file: " + storageKey, ex);
        }
    }

    // Resolves a storage key to an absolute path and ensures it stays inside uploadDir.
    private Path resolveAndGuard(String storageKey) {
        Path resolved = uploadDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(uploadDir)) {
            throw new FileStorageException("Invalid storage key — path traversal detected");
        }
        return resolved;
    }
}
