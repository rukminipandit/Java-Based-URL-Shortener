package com.urlshortener.service;

import com.urlshortener.dto.Dtos.*;
import com.urlshortener.model.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Core service interface for URL shortener operations.
 *
 * <p>All business logic for creating, resolving, and managing
 * shortened URLs is defined here. Implementations handle:
 * <ul>
 *   <li>Short code generation and uniqueness enforcement</li>
 *   <li>URL validation and normalization</li>
 *   <li>Password hashing for protected links</li>
 *   <li>Click recording and analytics dispatch</li>
 *   <li>API key validation</li>
 * </ul>
 * </p>
 */
public interface UrlService {

    /**
     * Creates a new short URL from the given request.
     *
     * @param request the creation request containing original URL, options, and optional custom code
     * @return response containing the short URL and metadata
     * @throws com.urlshortener.exception.Exceptions.InvalidUrlException if URL is malformed
     * @throws com.urlshortener.exception.Exceptions.ShortCodeAlreadyExistsException if custom code is taken
     * @throws com.urlshortener.exception.Exceptions.InvalidApiKeyException if API key is invalid
     */
    CreateUrlResponse createShortUrl(CreateUrlRequest request);

    /**
     * Resolves a short code to its original URL.
     * Does NOT record a click — use resolveAndRecord for redirect flows.
     *
     * @param shortCode the short code to look up
     * @return the UrlMapping entity
     * @throws com.urlshortener.exception.Exceptions.ShortCodeNotFoundException if not found
     * @throws com.urlshortener.exception.Exceptions.LinkDisabledException if link is disabled
     */
    UrlMapping resolve(String shortCode);

    /**
     * Resolves a short code and records a click event.
     * Password is verified if the link is protected.
     *
     * @param shortCode the short code to redirect
     * @param password  the password entered by the visitor (may be null)
     * @param ipAddress visitor's IP address
     * @param userAgent visitor's User-Agent string
     * @param referrer  HTTP Referer header value
     * @return the original URL to redirect to
     * @throws com.urlshortener.exception.Exceptions.InvalidPasswordException if password is wrong
     */
    String resolveAndRecord(String shortCode, String password, String ipAddress, String userAgent, String referrer);

    /**
     * Checks whether a given short code is available (not already in use).
     *
     * @param shortCode the code to check
     * @return true if available
     */
    boolean isShortCodeAvailable(String shortCode);

    /**
     * Retrieves a paginated list of all URL mappings for the admin dashboard.
     *
     * @param pageable pagination parameters
     * @return page of UrlMapping entities
     */
    Page<UrlMapping> getAllUrls(Pageable pageable);

    /**
     * Searches URL mappings by title, original URL, or short code.
     *
     * @param query    the search term
     * @param pageable pagination parameters
     * @return matching UrlMapping entities
     */
    Page<UrlMapping> searchUrls(String query, Pageable pageable);

    /**
     * Retrieves a single URL mapping by ID.
     *
     * @param id the mapping's database ID
     * @return the mapping, or empty if not found
     */
    Optional<UrlMapping> getById(Long id);

    /**
     * Enables or disables a short link.
     *
     * @param id     the mapping ID
     * @param active true to enable, false to disable
     */
    void setActive(Long id, boolean active);

    /**
     * Permanently deletes a short link and all its click events.
     *
     * @param id the mapping ID
     */
    void deleteUrl(Long id);

    /**
     * Builds the full short URL string for a given short code.
     *
     * @param shortCode the code
     * @return full URL string (e.g., "https://lv.io/abc123")
     */
    String buildShortUrl(String shortCode);

    /**
     * Returns the global dashboard statistics.
     */
    DashboardStats getDashboardStats();
}
