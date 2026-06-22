package com.college.placement.shared.filepipeline.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FilePipelineProperties.class)
public class FilePipelineConfig {
    // Properties binding only; beans live in the service layer.
}
