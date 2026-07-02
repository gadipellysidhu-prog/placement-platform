package com.college.placement.shared.storage;

import com.college.placement.shared.filepipeline.exception.FileStorageException;
import com.college.placement.shared.observability.metrics.StorageMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Object-storage {@link FileStorageProvider} built on the AWS SDK v2. Serves AWS
 * S3, MinIO and any S3-compatible store: set {@code storage.s3.endpoint} and
 * {@code path-style-access} for non-AWS backends. Generates time-limited
 * pre-signed download URLs.
 *
 * <p>Active when {@code storage.provider=s3}; the clients are created lazily so
 * no S3 connectivity is required for local/dev profiles.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3FileStorageProvider implements FileStorageProvider {

    private final StorageProperties props;
    private final StorageMetrics metrics;
    private S3Client s3;
    private S3Presigner presigner;
    private String bucket;

    public S3FileStorageProvider(StorageProperties props, StorageMetrics metrics) {
        this.props = props;
        this.metrics = metrics;
    }

    @PostConstruct
    public void init() {
        StorageProperties.S3 cfg = props.getS3();
        if (cfg.getBucket() == null || cfg.getBucket().isBlank()) {
            throw new IllegalStateException("storage.s3.bucket must be set when storage.provider=s3");
        }
        this.bucket = cfg.getBucket();
        Region region = Region.of(cfg.getRegion());
        AwsCredentialsProvider credentials = resolveCredentials(cfg);

        var s3Builder = S3Client.builder()
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(cfg.isPathStyleAccess())
                        .build());
        var presignerBuilder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentials);

        if (cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()) {
            URI endpoint = URI.create(cfg.getEndpoint());
            s3Builder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }

        this.s3 = s3Builder.build();
        this.presigner = presignerBuilder.build();
        log.info("Storage provider=s3 bucket={} endpoint={} pathStyle={}",
                bucket, cfg.getEndpoint(), cfg.isPathStyleAccess());
    }

    @PreDestroy
    public void shutdown() {
        if (s3 != null) s3.close();
        if (presigner != null) presigner.close();
    }

    @Override
    public String store(MultipartFile file) {
        String storageKey = UUID.randomUUID().toString();
        try {
            PutObjectRequest.Builder req = PutObjectRequest.builder().bucket(bucket).key(storageKey);
            if (file.getContentType() != null) {
                req.contentType(file.getContentType());
            }
            s3.putObject(req.build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            metrics.recordStore(providerId());
            return storageKey;
        } catch (IOException ex) {
            metrics.recordFailure(providerId(), "store");
            throw new FileStorageException("Failed to read uploaded file for S3 upload", ex);
        } catch (RuntimeException ex) {
            metrics.recordFailure(providerId(), "store");
            throw new FileStorageException("Failed to store object in S3", ex);
        }
    }

    @Override
    public String store(InputStream data, long sizeBytes, String contentType) {
        String storageKey = UUID.randomUUID().toString();
        try {
            PutObjectRequest.Builder req = PutObjectRequest.builder().bucket(bucket).key(storageKey);
            if (contentType != null) {
                req.contentType(contentType);
            }
            s3.putObject(req.build(), RequestBody.fromInputStream(data, sizeBytes));
            metrics.recordStore(providerId());
            return storageKey;
        } catch (RuntimeException ex) {
            metrics.recordFailure(providerId(), "store");
            throw new FileStorageException("Failed to store stream in S3", ex);
        }
    }

    @Override
    public Resource load(String storageKey) {
        return new S3ObjectResource(s3, bucket, storageKey);
    }

    @Override
    public InputStream openStream(String storageKey) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageKey).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
            metrics.recordDelete(providerId());
        } catch (RuntimeException ex) {
            metrics.recordFailure(providerId(), "delete");
            throw new FileStorageException("Failed to delete object from S3: " + storageKey, ex);
        }
    }

    @Override
    public Optional<String> generateSignedUrl(String storageKey, Duration ttl) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())
                .build();
        return Optional.of(presigner.presignGetObject(presignRequest).url().toString());
    }

    @Override
    public StoredObjectMetadata metadata(String storageKey) {
        try {
            HeadObjectResponse head =
                    s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageKey).build());
            return new StoredObjectMetadata(storageKey, head.contentLength(), head.contentType(), true);
        } catch (NoSuchKeyException ex) {
            return StoredObjectMetadata.absent(storageKey);
        }
    }

    @Override
    public String providerId() {
        return "s3";
    }

    private AwsCredentialsProvider resolveCredentials(StorageProperties.S3 cfg) {
        if (cfg.getAccessKey() != null && !cfg.getAccessKey().isBlank()
                && cfg.getSecretKey() != null && !cfg.getSecretKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
