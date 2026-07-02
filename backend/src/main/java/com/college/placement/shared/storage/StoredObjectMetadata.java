package com.college.placement.shared.storage;

/**
 * Provider-agnostic metadata about a stored object. {@code contentType} may be
 * {@code null} for backends that do not persist it (e.g. local disk).
 */
public record StoredObjectMetadata(
        String storageKey,
        long sizeBytes,
        String contentType,
        boolean exists
) {
    public static StoredObjectMetadata absent(String storageKey) {
        return new StoredObjectMetadata(storageKey, 0L, null, false);
    }
}
