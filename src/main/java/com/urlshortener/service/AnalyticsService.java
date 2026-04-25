package com.urlshortener.service;

import com.urlshortener.dto.Dtos.AnalyticsSummary;
import com.urlshortener.model.ClickEvent;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for click analytics operations.
 */
public interface AnalyticsService {

    /**
     * Records a click event for a URL mapping (async-safe).
     *
     * @param urlMappingId the ID of the mapping being clicked
     * @param ipAddress    visitor IP
     * @param userAgent    visitor User-Agent
     * @param referrer     HTTP Referer value
     */
    void recordClick(Long urlMappingId, String ipAddress, String userAgent, String referrer);

    /**
     * Builds a full analytics summary for a given URL mapping.
     *
     * @param urlMappingId the mapping ID
     * @param days         how many days of history to include
     * @return AnalyticsSummary DTO
     */
    AnalyticsSummary getAnalytics(Long urlMappingId, int days);

    /**
     * Extracts the real IP address from a request, accounting for proxies.
     *
     * @param request the HTTP request
     * @return best-guess real IP address string
     */
    String extractIpAddress(HttpServletRequest request);
}
