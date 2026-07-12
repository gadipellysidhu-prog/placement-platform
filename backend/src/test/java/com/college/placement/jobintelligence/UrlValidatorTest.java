package com.college.placement.jobintelligence;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import com.college.placement.modules.jobintelligence.crawler.UrlValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SSRF guard matrix — private/loopback/link-local targets must never be fetchable. */
class UrlValidatorTest {

    private UrlValidator validator(boolean allowPrivate) {
        var crawler = new JobIntelligenceProperties.Crawler(null, null, 0, 0, allowPrivate);
        return new UrlValidator(new JobIntelligenceProperties(true, crawler, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/jobs",
            "http://127.0.0.1/jobs",
            "http://[::1]/jobs",
            "http://10.0.0.5/jobs",
            "http://172.16.1.1/jobs",
            "http://192.168.1.10/jobs",
            "http://169.254.169.254/latest/meta-data", // cloud metadata endpoint
            "http://100.64.0.1/jobs",                  // CGNAT
            "http://0.0.0.0/jobs",
    })
    void rejectsNonPublicAddresses(String url) {
        assertThatThrownBy(() -> validator(false).validate(url))
                .isInstanceOf(UrlValidator.InvalidUrlException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/jobs",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "gopher://example.com",
    })
    void rejectsUnsupportedProtocols(String url) {
        assertThatThrownBy(() -> validator(false).validate(url))
                .isInstanceOf(UrlValidator.InvalidUrlException.class)
                .hasMessageContaining("http");
    }

    @Test
    void rejectsEmbeddedCredentials() {
        assertThatThrownBy(() -> validator(false).validate("https://user:pass@203.0.113.7/jobs"))
                .isInstanceOf(UrlValidator.InvalidUrlException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void rejectsBlankAndOversizedUrls() {
        assertThatThrownBy(() -> validator(false).validate("  "))
                .isInstanceOf(UrlValidator.InvalidUrlException.class);
        assertThatThrownBy(() -> validator(false).validate("https://x.io/" + "a".repeat(2048)))
                .isInstanceOf(UrlValidator.InvalidUrlException.class)
                .hasMessageContaining("2048");
    }

    @Test
    void acceptsPublicHttpsUrl() {
        // Public documentation IP (TEST-NET-3) is not in any private/reserved JDK check.
        assertThatCode(() -> validator(false).validate("https://203.0.113.10/careers/123"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowPrivateNetworksFlagOnlyRelaxesAddressChecks() {
        assertThatCode(() -> validator(true).validate("http://127.0.0.1:8099/job"))
                .doesNotThrowAnyException();
        // Protocol rules still apply even with the flag on.
        assertThatThrownBy(() -> validator(true).validate("file:///etc/passwd"))
                .isInstanceOf(UrlValidator.InvalidUrlException.class);
    }
}
