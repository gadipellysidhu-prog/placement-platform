package com.college.placement.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String PEM_HEADER_PATTERN = "-----[A-Z ]+-----";

    private final JwtProperties properties;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws Exception {
        String priv = properties.privateKeyPem();
        String pub  = properties.publicKeyPem();

        if (priv != null && !priv.isBlank() && pub != null && !pub.isBlank()) {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decodePem(priv)));
            publicKey  = (RSAPublicKey)  kf.generatePublic(new X509EncodedKeySpec(decodePem(pub)));
            log.info("JWT: RSA key pair loaded from configuration.");
        } else {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            privateKey = (RSAPrivateKey) pair.getPrivate();
            publicKey  = (RSAPublicKey)  pair.getPublic();
            log.warn("JWT: Using ephemeral RSA key pair. Set JWT_PRIVATE_KEY_PEM and JWT_PUBLIC_KEY_PEM for production.");
        }
    }

    public String generateAccessToken(String subject, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + properties.accessTokenExpiryMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey)
                .compact();
    }

    public Optional<Claims> validateAndParse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    private static byte[] decodePem(String pem) {
        String stripped = pem.replaceAll(PEM_HEADER_PATTERN, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(stripped);
    }
}
