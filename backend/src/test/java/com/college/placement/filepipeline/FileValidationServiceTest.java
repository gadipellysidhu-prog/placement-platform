package com.college.placement.filepipeline;

import com.college.placement.shared.filepipeline.config.FilePipelineProperties;
import com.college.placement.shared.filepipeline.exception.FileTooLargeException;
import com.college.placement.shared.filepipeline.exception.InvalidFileTypeException;
import com.college.placement.shared.filepipeline.service.FileValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationServiceTest {

    // Magic byte prefixes
    private static final byte[] PDF_MAGIC  = {0x25, 0x50, 0x44, 0x46, '-'};         // %PDF-
    private static final byte[] PNG_MAGIC  = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D}; // .PNG.
    private static final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0}; // JPEG SOI+APP0

    private FileValidationService service;

    @BeforeEach
    void setUp() {
        FilePipelineProperties props = new FilePipelineProperties();
        props.setMaxSizeBytes(1024);  // 1 KB — small limit for size tests
        props.setAllowedMimeTypes(List.of("application/pdf", "image/png", "image/jpeg"));
        service = new FileValidationService(props);
    }

    // ── Valid files ───────────────────────────────────────────────────────────

    @Test
    void validate_validPdf_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", PDF_MAGIC);
        assertThatNoException().isThrownBy(() -> service.validate(file));
    }

    @Test
    void validate_validPng_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.png", "image/png", PNG_MAGIC);
        assertThatNoException().isThrownBy(() -> service.validate(file));
    }

    @Test
    void validate_validJpeg_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", JPEG_MAGIC);
        assertThatNoException().isThrownBy(() -> service.validate(file));
    }

    // ── Rejected MIME types ───────────────────────────────────────────────────

    @Test
    void validate_executableMimeType_throwsInvalidFileTypeException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", new byte[]{0x4D, 0x5A});
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void validate_htmlMimeType_throwsInvalidFileTypeException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "page.html", "text/html", "<html>".getBytes());
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void validate_nullMimeType_throwsInvalidFileTypeException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "unknown", null, PDF_MAGIC);
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    // ── File size ─────────────────────────────────────────────────────────────

    @Test
    void validate_oversizedFile_throwsFileTooLargeException() {
        byte[] oversized = new byte[2048]; // > 1 KB limit
        oversized[0] = PDF_MAGIC[0]; oversized[1] = PDF_MAGIC[1];
        oversized[2] = PDF_MAGIC[2]; oversized[3] = PDF_MAGIC[3];

        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", oversized);
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(FileTooLargeException.class);
    }

    // ── Magic byte mismatch ───────────────────────────────────────────────────

    @Test
    void validate_pdfMimeButPngBytes_throwsInvalidFileTypeException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "trick.pdf", "application/pdf", PNG_MAGIC);
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void validate_emptyFile_throwsInvalidFileTypeException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileTypeException.class);
    }
}
