package com.college.placement.shared.storage;

import org.springframework.core.io.AbstractResource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.InputStream;

/**
 * A Spring {@link org.springframework.core.io.Resource} backed by an S3 object.
 * Each {@link #getInputStream()} issues a fresh {@code GetObject}, so the
 * resource is safely re-readable (e.g. scanned then streamed to a client).
 */
class S3ObjectResource extends AbstractResource {

    private final S3Client s3;
    private final String bucket;
    private final String key;

    S3ObjectResource(S3Client s3, String bucket, String key) {
        this.s3 = s3;
        this.bucket = bucket;
        this.key = key;
    }

    @Override
    public InputStream getInputStream() {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public boolean exists() {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        }
    }

    @Override
    public long contentLength() {
        HeadObjectResponse head =
                s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
        return head.contentLength();
    }

    @Override
    public String getFilename() {
        return key;
    }

    @Override
    public String getDescription() {
        return "S3 object [" + bucket + "/" + key + "]";
    }
}
