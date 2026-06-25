package com.college.placement.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    long accessTokenExpiryMs,
    long refreshTokenExpiryMs,
    String privateKeyPem,
    String publicKeyPem,
    String issuer,
    String audience
) {}
