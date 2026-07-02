package com.college.placement.shared.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables {@link StorageProperties} binding. The concrete {@link FileStorageProvider}
 * bean is selected by {@code storage.provider} via {@code @ConditionalOnProperty}
 * on the provider implementations.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
}
