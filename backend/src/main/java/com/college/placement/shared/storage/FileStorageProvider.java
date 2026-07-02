package com.college.placement.shared.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * Storage abstraction. Business code interacts exclusively with this interface
 * so the underlying backend (local disk, MinIO, AWS S3, S3-compatible, ...) can
 * be swapped purely through configuration. Implementations must treat the
 * {@code storageKey} as opaque and guard against path traversal.
 */
public interface FileStorageProvider {

    /**
     * Persists a multipart upload under a freshly generated storage key.
     *
     * @return the opaque storage key identifying the stored object
     */
    String store(MultipartFile file);

    /**
     * Persists raw bytes from a stream under a freshly generated storage key.
     * The caller must supply the exact byte length and (optionally) content type.
     *
     * @return the opaque storage key identifying the stored object
     */
    String store(InputStream data, long sizeBytes, String contentType);

    /** Resolves a storage key to a re-readable {@link Resource} for streaming. */
    Resource load(String storageKey);

    /** Opens a fresh input stream for the stored object. Caller must close it. */
    InputStream openStream(String storageKey);

    /** Whether an object exists for the given key. */
    boolean exists(String storageKey);

    /** Removes the object; succeeds silently if already absent. */
    void delete(String storageKey);

    /**
     * Generates a time-limited signed download URL where supported.
     * Returns {@link Optional#empty()} for backends without presigning
     * (e.g. local disk), signalling callers to stream via the application.
     */
    Optional<String> generateSignedUrl(String storageKey, Duration ttl);

    /** Object metadata, or {@link StoredObjectMetadata#absent} if missing. */
    StoredObjectMetadata metadata(String storageKey);

    /** Stable identifier of the active provider (e.g. {@code local}, {@code s3}). */
    String providerId();
}
