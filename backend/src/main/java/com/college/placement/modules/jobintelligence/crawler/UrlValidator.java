package com.college.placement.modules.jobintelligence.crawler;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Validates officer-supplied job URLs before any network access. This is the
 * SSRF guard: only http(s), only public unicast addresses — every DNS answer for
 * the host is checked so a hostname cannot smuggle in a private/loopback target.
 * Redirect hops are re-validated by the fetcher with this same validator.
 *
 * <p>{@code job.intelligence.crawler.allow-private-networks} (default false)
 * disables only the address checks — used by local test fixtures.
 */
@Component
public class UrlValidator {

    private final JobIntelligenceProperties properties;

    public UrlValidator(JobIntelligenceProperties properties) {
        this.properties = properties;
    }

    /** Validates and normalizes; throws {@link InvalidUrlException} on any violation. */
    public URI validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("URL is required");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > 2048) {
            throw new InvalidUrlException("URL exceeds 2048 characters");
        }

        final URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("Malformed URL");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidUrlException("Only http and https URLs are supported");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("URL has no host");
        }
        if (uri.getUserInfo() != null) {
            throw new InvalidUrlException("URLs with embedded credentials are not allowed");
        }

        if (!properties.crawler().allowPrivateNetworks()) {
            assertPubliclyRoutable(host);
        }
        return uri;
    }

    private void assertPubliclyRoutable(String host) {
        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new InvalidUrlException("Host could not be resolved");
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress()
                    || address.isAnyLocalAddress()
                    || address.isSiteLocalAddress()      // RFC1918: 10/8, 172.16/12, 192.168/16
                    || address.isLinkLocalAddress()      // 169.254/16 incl. cloud metadata
                    || address.isMulticastAddress()) {
                throw new InvalidUrlException("URL resolves to a non-public address");
            }
            byte[] raw = address.getAddress();
            // 100.64/10 (CGNAT) and 0/8 are not covered by the JDK predicates.
            if (raw.length == 4) {
                int first = raw[0] & 0xFF;
                int second = raw[1] & 0xFF;
                if (first == 0 || (first == 100 && second >= 64 && second <= 127)) {
                    throw new InvalidUrlException("URL resolves to a non-public address");
                }
            }
        }
    }

    /** Non-retryable validation failure — the URL itself is unacceptable. */
    public static class InvalidUrlException extends RuntimeException {
        public InvalidUrlException(String message) {
            super(message);
        }
    }
}
