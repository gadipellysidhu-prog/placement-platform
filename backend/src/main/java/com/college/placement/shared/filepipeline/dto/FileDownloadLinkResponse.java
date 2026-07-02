package com.college.placement.shared.filepipeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Describes how to download a file. When the active storage provider supports
 * presigning, {@code signed} is {@code true} and {@code url} is a time-limited
 * direct link to object storage. Otherwise {@code signed} is {@code false} and
 * {@code url} points at the application's streaming endpoint.
 */
@Schema(description = "Download link for a stored file")
public record FileDownloadLinkResponse(
        UUID id,
        String url,
        boolean signed
) {
    public static FileDownloadLinkResponse signed(UUID id, String url) {
        return new FileDownloadLinkResponse(id, url, true);
    }

    public static FileDownloadLinkResponse stream(UUID id) {
        return new FileDownloadLinkResponse(id, "/api/files/" + id, false);
    }
}
