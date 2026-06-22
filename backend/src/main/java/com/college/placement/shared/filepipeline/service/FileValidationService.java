package com.college.placement.shared.filepipeline.service;

import com.college.placement.shared.filepipeline.config.FilePipelineProperties;
import com.college.placement.shared.filepipeline.exception.FileStorageException;
import com.college.placement.shared.filepipeline.exception.FileTooLargeException;
import com.college.placement.shared.filepipeline.exception.InvalidFileTypeException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

@Service
public class FileValidationService {

    // Magic byte signatures for each allowed MIME type.
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46},             // %PDF
            "image/png",       new byte[]{(byte)0x89, 0x50, 0x4E, 0x47},      // .PNG
            "image/jpeg",      new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF}  // JPEG SOI
    );

    private final FilePipelineProperties props;

    public FileValidationService(FilePipelineProperties props) {
        this.props = props;
    }

    public void validate(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file);
        validateMimeType(file);
        validateMagicBytes(file);
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileTypeException("empty");
        }
    }

    private void validateSize(MultipartFile file) {
        if (file.getSize() > props.getMaxSizeBytes()) {
            throw new FileTooLargeException(file.getSize(), props.getMaxSizeBytes());
        }
    }

    private void validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !props.getAllowedMimeTypes().contains(contentType)) {
            throw new InvalidFileTypeException(contentType != null ? contentType : "unknown");
        }
    }

    private void validateMagicBytes(MultipartFile file) {
        String contentType = file.getContentType();
        byte[] expectedMagic = MAGIC_BYTES.get(contentType);
        if (expectedMagic == null) {
            // No magic check defined for this type; MIME validation already passed.
            return;
        }

        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[expectedMagic.length];
            int read = in.readNBytes(header, 0, header.length);
            if (read < expectedMagic.length
                    || !Arrays.equals(header, 0, read, expectedMagic, 0, expectedMagic.length)) {
                // File content does not match the declared MIME type.
                throw new InvalidFileTypeException(contentType);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read file header for validation", ex);
        }
    }
}
