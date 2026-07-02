package com.college.placement.modules.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds auth-module configuration properties.
 */
@Configuration
@EnableConfigurationProperties(VerificationProperties.class)
public class AuthConfig {
}
