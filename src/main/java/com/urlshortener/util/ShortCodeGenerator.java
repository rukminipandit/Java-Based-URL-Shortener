package com.urlshortener.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class responsible for generating and validating short codes.
 *
 * <p>Uses a Base62 alphabet (a-zA-Z0-9) to generate URL-safe short codes.
 * The generation strategy uses SecureRandom to ensure unpredictability.
 * Additionally supports hash-based generation from the original URL.</p>
 */
@Component
public class ShortCodeGenerator {

    private static final String BASE62_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.short-code-length:6}")
    private int defaultLength;

    /**
     * Generates a random Base62 short code of the configured default length.
     *
     * @return a random short code string
     */
    public String generateRandom() {
        return generateRandom(defaultLength);
    }

    /**
     * Generates a random Base62 short code of the given length.
     *
     * @param length desired length of the short code
     * @return a random short code string
     */
    public String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_ALPHABET.charAt(SECURE_RANDOM.nextInt(BASE62_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a deterministic short code from a URL using MD5 hashing + Base62 encoding.
     * Useful for deduplication when the same URL is submitted multiple times.
     *
     * @param originalUrl the original URL to hash
     * @return a Base62-encoded short code derived from the URL
     */
    public String generateFromUrl(String originalUrl) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(originalUrl.getBytes());
            // Use first 4 bytes to produce a Base62 string of ~6 chars
            long value = ((long)(hash[0] & 0xFF) << 24)
                       | ((long)(hash[1] & 0xFF) << 16)
                       | ((long)(hash[2] & 0xFF) <<  8)
                       |  (long)(hash[3] & 0xFF);
            return encodeBase62(Math.abs(value));
        } catch (NoSuchAlgorithmException e) {
            return generateRandom();
        }
    }

    /**
     * Encodes a long integer value to a Base62 string.
     *
     * @param value the value to encode
     * @return Base62-encoded string, padded to at least 6 characters
     */
    public String encodeBase62(long value) {
        if (value == 0) return "a";
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62_ALPHABET.charAt((int)(value % 62)));
            value /= 62;
        }
        while (sb.length() < defaultLength) sb.append('a');
        return sb.reverse().toString();
    }

    /**
     * Generates an API key with the "lv_sk_" prefix and 32 bytes of random data.
     *
     * @return a new unique API key string
     */
    public String generateApiKey() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "lv_sk_" + raw;
    }

    /**
     * Validates that a custom short code uses only allowed characters (a-zA-Z0-9_-).
     *
     * @param code the code to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCustomCode(String code) {
        if (code == null || code.isBlank()) return false;
        if (code.length() < 3 || code.length() > 30) return false;
        return code.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Sanitizes a custom short code by trimming whitespace and converting to lowercase.
     *
     * @param code the raw code input
     * @return the sanitized code
     */
    public String sanitizeCode(String code) {
        if (code == null) return null;
        return code.trim().toLowerCase();
    }
}
