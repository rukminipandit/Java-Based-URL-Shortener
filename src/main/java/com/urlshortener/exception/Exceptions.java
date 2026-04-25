package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * All custom exceptions for the URL shortener.
 */
public final class Exceptions {

    private Exceptions() {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ShortCodeNotFoundException extends RuntimeException {
        public ShortCodeNotFoundException(String shortCode) {
            super("Short URL not found: " + shortCode);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ShortCodeAlreadyExistsException extends RuntimeException {
        public ShortCodeAlreadyExistsException(String shortCode) {
            super("Short code already in use: " + shortCode);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidApiKeyException extends RuntimeException {
        public InvalidApiKeyException() {
            super("Invalid or revoked API key");
        }
        public InvalidApiKeyException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException() {
            super("Incorrect password for this link");
        }
    }

    @ResponseStatus(HttpStatus.GONE)
    public static class LinkDisabledException extends RuntimeException {
        public LinkDisabledException(String shortCode) {
            super("This link has been disabled: " + shortCode);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidUrlException extends RuntimeException {
        public InvalidUrlException(String url) {
            super("Invalid URL format: " + url);
        }
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException() {
            super("Rate limit exceeded for this API key");
        }
    }
}
