package com.urlshortener.controller;

import com.urlshortener.dto.Dtos.*;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for programmatic access to the URL shortener.
 * All endpoints require a valid API key via X-API-Key header or ?apiKey= param.
 * Authentication is enforced by ApiKeyAuthFilter upstream.
 *
 * <p>Base path: /api/v1</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    public ApiController(UrlService urlService, AnalyticsService analyticsService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
    }

    // ─── POST /api/v1/shorten ────────────────────────────────────────────────────

    /**
     * Creates a new short URL.
     *
     * Request body:
     * <pre>{@code
     * {
     *   "originalUrl": "https://example.com/very/long/path",
     *   "customCode": "my-link",          // optional
     *   "title": "My Link",               // optional
     *   "analyticsEnabled": true,         // optional, default true
     *   "passwordProtected": false,       // optional, default false
     *   "password": "secret"              // required if passwordProtected=true
     * }
     * }</pre>
     */
    @PostMapping("/shorten")
    public ResponseEntity<CreateUrlResponse> shorten(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest httpRequest) {

        // Inject validated API key from filter attribute
        String apiKey = (String) httpRequest.getAttribute("validatedApiKey");
        request.setApiKey(apiKey);

        CreateUrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── GET /api/v1/links ───────────────────────────────────────────────────────

    /**
     * Lists all short links created with this API key.
     * Supports pagination: ?page=0&size=20&sort=createdAt&dir=desc
     */
    @GetMapping("/links")
    public ResponseEntity<Page<UrlMapping>> listLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            HttpServletRequest httpRequest) {

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String apiKey = (String) httpRequest.getAttribute("validatedApiKey");

        PageRequest pageable = PageRequest.of(
            Math.min(page, 100),
            Math.min(size, 100),
            Sort.by(direction, sort)
        );

        Page<UrlMapping> results = urlService.getAllUrls(pageable);
        return ResponseEntity.ok(results);
    }

    // ─── GET /api/v1/links/{id} ──────────────────────────────────────────────────

    /**
     * Retrieves a single link by its database ID.
     */
    @GetMapping("/links/{id}")
    public ResponseEntity<UrlMapping> getLink(@PathVariable Long id) {
        return urlService.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── DELETE /api/v1/links/{id} ───────────────────────────────────────────────

    /**
     * Permanently deletes a link and all its analytics data.
     */
    @DeleteMapping("/links/{id}")
    public ResponseEntity<Map<String, Object>> deleteLink(@PathVariable Long id) {
        urlService.deleteUrl(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Link deleted successfully"));
    }

    // ─── PATCH /api/v1/links/{id}/toggle ────────────────────────────────────────

    /**
     * Enables or disables a link.
     * Body: {"active": true/false}
     */
    @PatchMapping("/links/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleLink(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        boolean active = Boolean.TRUE.equals(body.get("active"));
        urlService.setActive(id, active);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "id", id,
            "active", active
        ));
    }

    // ─── GET /api/v1/links/{id}/analytics ────────────────────────────────────────

    /**
     * Returns analytics for a specific link.
     * Query param: ?days=30 (default 30, max 365)
     */
    @GetMapping("/links/{id}/analytics")
    public ResponseEntity<AnalyticsSummary> getAnalytics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {

        int clampedDays = Math.min(Math.max(days, 1), 365);
        AnalyticsSummary summary = analyticsService.getAnalytics(id, clampedDays);
        return ResponseEntity.ok(summary);
    }

    // ─── GET /api/v1/check/{code} ────────────────────────────────────────────────

    /**
     * Checks whether a custom short code is available.
     */
    @GetMapping("/check/{code}")
    public ResponseEntity<Map<String, Object>> checkCode(@PathVariable String code) {
        boolean available = urlService.isShortCodeAvailable(code);
        return ResponseEntity.ok(Map.of(
            "code", code,
            "available", available
        ));
    }

    // ─── GET /api/v1/stats ───────────────────────────────────────────────────────

    /**
     * Returns global dashboard statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(urlService.getDashboardStats());
    }
}
