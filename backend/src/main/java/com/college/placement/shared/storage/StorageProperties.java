package com.college.placement.shared.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration for the pluggable file-storage subsystem. The active provider is
 * chosen by {@code storage.provider} ({@code local} | {@code s3}); the {@code s3}
 * adapter also serves MinIO and any S3-compatible store via {@code endpoint} +
 * {@code path-style-access}. Switching providers requires no code changes.
 */
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {

    /** Active provider id: {@code local} (default) or {@code s3}. */
    private String provider = "local";

    /** Default TTL (seconds) for generated signed download URLs. */
    private long signedUrlTtlSeconds = 900;

    @NestedConfigurationProperty
    private Local local = new Local();

    @NestedConfigurationProperty
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        /** Root directory for on-disk storage. */
        private String dir = "./uploads";
    }

    @Getter
    @Setter
    public static class S3 {
        /** Bucket / container name. */
        private String bucket;
        /** AWS region (also required by MinIO clients; any value for non-AWS). */
        private String region = "us-east-1";
        /** Custom endpoint for MinIO or S3-compatible stores; empty for AWS S3. */
        private String endpoint;
        /** Path-style access — required by MinIO and most S3-compatible stores. */
        private boolean pathStyleAccess = true;
        /** Static access key; if blank the default AWS credentials chain is used. */
        private String accessKey;
        /** Static secret key; if blank the default AWS credentials chain is used. */
        private String secretKey;
    }
}
