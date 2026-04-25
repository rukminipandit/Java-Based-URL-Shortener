package com.urlshortener.repository;

import com.urlshortener.model.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for UrlMapping entities.
 * Provides all data access operations for short URL management.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Find a URL mapping by its short code (case-insensitive).
     */
    Optional<UrlMapping> findByShortCodeIgnoreCase(String shortCode);

    /**
     * Find only active mappings by short code.
     */
    @Query("SELECT u FROM UrlMapping u WHERE LOWER(u.shortCode) = LOWER(:shortCode) AND u.active = true")
    Optional<UrlMapping> findActiveByShortCode(@Param("shortCode") String shortCode);

    /**
     * Check if a short code already exists.
     */
    boolean existsByShortCodeIgnoreCase(String shortCode);

    /**
     * Find all mappings for a given API key, paginated.
     */
    Page<UrlMapping> findByApiKeyOrderByCreatedAtDesc(String apiKey, Pageable pageable);

    /**
     * Search mappings by title or original URL (for admin dashboard).
     */
    @Query("SELECT u FROM UrlMapping u WHERE " +
           "LOWER(u.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.originalUrl) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.shortCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY u.createdAt DESC")
    Page<UrlMapping> searchByQuery(@Param("query") String query, Pageable pageable);

    /**
     * Get all mappings ordered by click count descending (top links).
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.active = true ORDER BY u.clickCount DESC")
    List<UrlMapping> findTopByClickCount(Pageable pageable);

    /**
     * Count total active links.
     */
    long countByActiveTrue();

    /**
     * Count total inactive links.
     */
    long countByActiveFalse();

    /**
     * Count links with analytics enabled.
     */
    long countByAnalyticsEnabledTrue();

    /**
     * Count links with password protection.
     */
    long countByPasswordProtectedTrue();

    /**
     * Sum of all click counts across all URLs.
     */
    @Query("SELECT COALESCE(SUM(u.clickCount), 0) FROM UrlMapping u")
    long sumAllClickCounts();

    /**
     * Find links created within a date range.
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.createdAt BETWEEN :start AND :end ORDER BY u.createdAt DESC")
    List<UrlMapping> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Bulk toggle active status.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = :active WHERE u.id IN :ids")
    int bulkSetActive(@Param("ids") List<Long> ids, @Param("active") boolean active);

    /**
     * Increment click count directly in DB (atomic operation).
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = :now WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * Find all links created by API key with analytics enabled.
     */
    List<UrlMapping> findByApiKeyAndAnalyticsEnabledTrue(String apiKey);

    /**
     * Find links that have never been accessed (clickCount = 0).
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.clickCount = 0 AND u.createdAt < :before ORDER BY u.createdAt ASC")
    List<UrlMapping> findUnusedLinksBefore(@Param("before") LocalDateTime before);
}
