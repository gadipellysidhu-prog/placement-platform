package com.college.placement.shared.filepipeline.service;

import com.college.placement.shared.filepipeline.config.FilePipelineProperties;
import com.college.placement.shared.filepipeline.exception.FileStorageException;
import com.college.placement.shared.storage.FileStorageProvider;
import com.college.placement.shared.storage.StoredObjectMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * On-disk {@link FileStorageProvider} — the default storage backend, suitable for
 * development and single-node deployments. Active when {@code storage.provider=local}
 * (the default). Object storage (S3 / MinIO / S3-compatible) is provided by
 * {@code S3FileStorageProvider} when {@code storage.provider=s3}.
 *
 * <p>Storage location is driven by {@code file.pipeline.upload-dir}. Signed URLs are
 * unsupported here (returns empty), so callers stream local files through the app.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class FileStorageService implements FileStorageProvider {

    private final FilePipelineProperties props;
    private Path uploadDir;

    public FileStorageService(FilePipelineProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Path.of(props.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        log.info("Storage provider=local directory={}", uploadDir);
    }

    /**
     * Persists the multipart file under a UUID-based storage key.
     *
     * @return the storage key (UUID string) that uniquely identifies the file on disk
     */
    @Override
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

    @Override
    public String store(InputStream data, long sizeBytes, String contentType) {
        String storageKey = UUID.randomUUID().toString();
        Path target = resolveAndGuard(storageKey);
        try {
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to persist stream to disk", ex);
        }
    }

    /**
     * Resolves a storage key to a Spring {@link Resource} suitable for streaming.
     */
    @Override
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

    @Override
    public InputStream openStream(String storageKey) {
        try {
            return load(storageKey).getInputStream();
        } catch (IOException ex) {
            throw new FileStorageException("Failed to open file stream: " + storageKey, ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolveAndGuard(storageKey));
    }

    /**
     * Removes the physical file associated with the given storage key.
     * Silently succeeds if the file is already absent.
     */
    @Override
    public void delete(String storageKey) {
        Path target = resolveAndGuard(storageKey);
        try {
            boolean deleted = Files.deleteIfExists(target);
            log.debug("Deleted file: storageKey={} existed={}", storageKey, deleted);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete file: " + storageKey, ex);
        }
    }

    @Override
    public Optional<String> generateSignedUrl(String storageKey, Duration ttl) {
        // Local disk has no presigning; callers stream via the application.
        return Optional.empty();
    }

    @Override
    public StoredObjectMetadata metadata(String storageKey) {
        Path target = resolveAndGuard(storageKey);
        if (!Files.exists(target)) {
            return StoredObjectMetadata.absent(storageKey);
        }
        try {
            return new StoredObjectMetadata(storageKey, Files.size(target), null, true);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read file metadata: " + storageKey, ex);
        }
    }

    @Override
    public String providerId() {
        return "local";
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
