package com.college.placement.modules.notification.config;

import com.college.placement.shared.notification.email.NotificationEmailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables notification-related configuration properties binding.
 */
@Configuration
@EnableConfigurationProperties(NotificationEmailProperties.class)
public class NotificationConfig {
}
