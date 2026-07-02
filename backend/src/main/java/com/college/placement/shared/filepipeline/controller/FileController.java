package com.college.placement.shared.filepipeline.controller;

import com.college.placement.shared.filepipeline.domain.FileScanRecord;
import com.college.placement.shared.filepipeline.dto.FileDownloadLinkResponse;
import com.college.placement.shared.filepipeline.dto.FileUploadResponse;
import com.college.placement.shared.filepipeline.service.FilePipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FilePipelineService pipelineService;

    public FileController(FilePipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    /**
     * POST /api/files/upload
     * <p>
     * Accepts: multipart/form-data with a field named {@code file}.
     * Returns 201 Created with file metadata including scan status and SHA-256 hash.
     * All authenticated roles may upload.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'PLACEMENT_OFFICER')")
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        String username = principal.getName();
        FileUploadResponse response = pipelineService.upload(file, username);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * GET /api/files/{id}
     * <p>
     * Streams the file with correct Content-Type and Content-Disposition headers.
     * Quarantined files return 403.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        FileScanRecord record = pipelineService.findRecord(id);
        Resource resource    = pipelineService.download(id);

        MediaType mediaType = parseMediaType(record.getContentType());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(record.getFilename())
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    /**
     * GET /api/files/{id}/link
     * <p>
     * Returns a time-limited signed URL when the active storage provider supports
     * presigning (S3 / MinIO), letting clients download directly from object storage.
     * Falls back to the streaming endpoint for local disk. Quarantined files return 403.
     */
    @GetMapping("/{id}/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileDownloadLinkResponse> downloadLink(@PathVariable UUID id) {
        return ResponseEntity.ok(pipelineService.signedDownloadUrl(id)
                .map(url -> FileDownloadLinkResponse.signed(id, url))
                .orElseGet(() -> FileDownloadLinkResponse.stream(id)));
    }

    /**
     * DELETE /api/files/{id}
     * <p>
     * Removes the physical file and its metadata record.
     * Restricted to ADMIN and PLACEMENT_OFFICER.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLACEMENT_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pipelineService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Falls back to application/octet-stream for any unrecognised or null content-type.
    private MediaType parseMediaType(String contentType) {
        if (contentType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
