package com.college.placement.modules.jobintelligence.crawler;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Fetches the raw HTML of an official job URL. Redirects are followed manually
 * (max N hops) so every hop is re-validated by {@link UrlValidator} — an initial
 * public URL can never redirect the crawler onto an internal address. The body
 * is size-capped while streaming; oversized pages are rejected, not truncated
 * silently into misleading content.
 */
@Slf4j
@Component
public class PageFetcher {

    private static final String USER_AGENT =
            "PlacementPlatformBot/1.0 (+job-intelligence; contact: placement office)";

    private final HttpClient httpClient;
    private final UrlValidator urlValidator;
    private final JobIntelligenceProperties properties;

    public PageFetcher(UrlValidator urlValidator, JobIntelligenceProperties properties) {
        this.urlValidator = urlValidator;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER) // manual, re-validated hops
                .connectTimeout(properties.crawler().connectTimeout())
                .build();
    }

    /** Fetch the page, following at most maxRedirects validated hops. */
    public String fetch(String rawUrl) {
        URI current = urlValidator.validate(rawUrl);
        int hops = 0;
        while (true) {
            FetchOutcome outcome = fetchOnce(current);
            if (outcome.redirectTo() == null) {
                return outcome.body();
            }
            if (++hops > properties.crawler().maxRedirects()) {
                throw new FetchException("Too many redirects", false);
            }
            current = urlValidator.validate(outcome.redirectTo());
            log.debug("JOB_INTEL event=REDIRECT hop={} to={}", hops, current.getHost());
        }
    }

    private FetchOutcome fetchOnce(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.crawler().readTimeout())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();

        final HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException ex) {
            throw new FetchException("Network error fetching page: " + ex.getMessage(), true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FetchException("Fetch interrupted", true);
        }

        int status = response.statusCode();
        if (status >= 300 && status < 400) {
            String location = response.headers().firstValue("Location")
                    .orElseThrow(() -> new FetchException("Redirect without Location header", false));
            drain(response);
            return new FetchOutcome(null, resolveRedirect(uri, location));
        }
        if (status == 404 || status == 410) {
            throw new FetchException("Job page not found (HTTP " + status + ")", false);
        }
        if (status == 429 || status >= 500) {
            throw new FetchException("Upstream unavailable (HTTP " + status + ")", true);
        }
        if (status != 200) {
            throw new FetchException("Unexpected HTTP status " + status, false);
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("text/html")
                .toLowerCase(Locale.ROOT);
        if (!contentType.contains("html") && !contentType.contains("text/plain")) {
            drain(response);
            throw new FetchException("Unsupported content type: " + contentType, false);
        }
        return new FetchOutcome(readCapped(response.body()), null);
    }

    private String readCapped(InputStream body) {
        int cap = properties.crawler().maxBodyBytes();
        try (body) {
            byte[] buffer = body.readNBytes(cap + 1);
            if (buffer.length > cap) {
                throw new FetchException("Page exceeds maximum size of " + cap + " bytes", false);
            }
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new FetchException("Error reading page body: " + ex.getMessage(), true);
        }
    }

    private static String resolveRedirect(URI base, String location) {
        try {
            return base.resolve(location).toString();
        } catch (IllegalArgumentException ex) {
            throw new FetchException("Malformed redirect location", false);
        }
    }

    private static void drain(HttpResponse<InputStream> response) {
        try (InputStream in = response.body()) {
            in.skip(Long.MAX_VALUE);
        } catch (IOException ignored) {
            // best-effort cleanup only
        }
    }

    private record FetchOutcome(String body, String redirectTo) {}

    /** Fetch failure; {@code transientFailure} controls whether the pipeline may retry. */
    public static class FetchException extends RuntimeException {
        private final boolean transientFailure;

        public FetchException(String message, boolean transientFailure) {
            super(message);
            this.transientFailure = transientFailure;
        }

        public boolean isTransient() {
            return transientFailure;
        }
    }
}
