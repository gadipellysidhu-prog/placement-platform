package com.college.placement.shared.filepipeline.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "file.pipeline")
@Getter
@Setter
public class FilePipelineProperties {

    private String uploadDir = "./uploads";
    private long maxSizeBytes = 10L * 1024 * 1024; // 10 MB
    private List<String> allowedMimeTypes = List.of("application/pdf", "image/png", "image/jpeg");
    private String clamAvHost = "localhost";
    private int clamAvPort = 3310;
    private int clamAvTimeoutMs = 30_000;
    private boolean scanEnabled = true;
}
