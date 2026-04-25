package com.urlshortener.util;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates and normalizes URLs before storing them as shortened links.
 *
 * <p>Performs both syntactic validation (regex) and semantic checks
 * (reachable scheme, non-private IPs, blocked domains).</p>
 */
@Component
public class UrlValidator {

    private static final Pattern URL_REGEX = Pattern.compile(
        "^(https?://)?" +
        "([\\w.-]+\\.\\w{2,})" +
        "([/?#][^\\s]*)?" +
        "$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Domains that are blocked from being shortened (to prevent abuse).
     */
    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "10.0.0.0",
        "192.168.",
        "bit.ly",
        "tinyurl.com"
    );

    /**
     * Maximum URL length allowed.
     */
    private static final int MAX_URL_LENGTH = 2048;

    /**
     * Validates whether the given string is a usable URL for shortening.
     *
     * @param url the raw URL string from the user
     * @return a ValidationResult with isValid flag and optional error message
     */
    public ValidationResult validate(String url) {
        if (url == null || url.isBlank()) {
            return ValidationResult.invalid("URL cannot be empty");
        }

        String trimmed = url.trim();

        if (trimmed.length() > MAX_URL_LENGTH) {
            return ValidationResult.invalid("URL is too long (max " + MAX_URL_LENGTH + " characters)");
        }

        // Auto-prepend https:// if missing scheme
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }

        // Basic regex check
        if (!URL_REGEX.matcher(trimmed).matches()) {
            return ValidationResult.invalid("URL format is invalid");
        }

        // Parse with Java URL for deeper validation
        try {
            URI uri = new URI(trimmed);
            URL parsed = uri.toURL();

            String scheme = parsed.getProtocol();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return ValidationResult.invalid("Only HTTP and HTTPS URLs are supported");
            }

            String host = parsed.getHost().toLowerCase();

            // Check blocked domains
            for (String blocked : BLOCKED_DOMAINS) {
                if (host.equals(blocked) || host.startsWith(blocked)) {
                    return ValidationResult.invalid("This URL cannot be shortened");
                }
            }

        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return ValidationResult.invalid("URL is not well-formed: " + e.getMessage());
        }

        return ValidationResult.valid(trimmed);
    }

    /**
     * Normalizes a URL: trims, lowercases the scheme and host, preserves path case.
     *
     * @param url the raw URL
     * @return normalized URL string
     */
    public String normalize(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme().toLowerCase();
            String host = uri.getHost().toLowerCase();
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            String fragment = uri.getRawFragment();

            StringBuilder normalized = new StringBuilder(scheme).append("://").append(host);
            if (path != null && !path.isEmpty()) normalized.append(path);
            if (query != null) normalized.append("?").append(query);
            if (fragment != null) normalized.append("#").append(fragment);
            return normalized.toString();
        } catch (Exception e) {
            return trimmed;
        }
    }

    // ─── Result Class ────────────────────────────────────────────────────────────

    public static class ValidationResult {
        private final boolean valid;
        private final String normalizedUrl;
        private final String error;

        private ValidationResult(boolean valid, String normalizedUrl, String error) {
            this.valid = valid;
            this.normalizedUrl = normalizedUrl;
            this.error = error;
        }

        public static ValidationResult valid(String normalizedUrl) {
            return new ValidationResult(true, normalizedUrl, null);
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, null, error);
        }

        public boolean isValid() { return valid; }
        public String getNormalizedUrl() { return normalizedUrl; }
        public String getError() { return error; }
    }
}
