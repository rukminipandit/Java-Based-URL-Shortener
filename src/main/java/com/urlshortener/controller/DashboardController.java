package com.urlshortener.controller;

import com.urlshortener.dto.Dtos.AnalyticsSummary;
import com.urlshortener.dto.Dtos.DashboardStats;
import com.urlshortener.model.ApiKey;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.ApiKeyService;
import com.urlshortener.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Admin dashboard controller.
 * All routes are under /admin/** and require ROLE_ADMIN.
 *
 * <p>Provides:
 * <ul>
 *   <li>Login/logout</li>
 *   <li>Dashboard overview with stats</li>
 *   <li>Link management (list, search, toggle, delete)</li>
 *   <li>Per-link analytics view</li>
 *   <li>API key management</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/admin")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final int PAGE_SIZE = 15;

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final ApiKeyService apiKeyService;

    public DashboardController(UrlService urlService,
                                AnalyticsService analyticsService,
                                ApiKeyService apiKeyService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.apiKeyService = apiKeyService;
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("loginError", error != null);
        return "admin/login";
    }

    // ─── Dashboard Overview ──────────────────────────────────────────────────────

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardStats stats = urlService.getDashboardStats();
        model.addAttribute("stats", stats);

        // Top 5 links
        Page<UrlMapping> recentLinks = urlService.getAllUrls(
            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("recentLinks", recentLinks.getContent());

        return "admin/dashboard";
    }

    // ─── Links Management ────────────────────────────────────────────────────────

    @GetMapping("/links")
    public String listLinks(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String query,
                             @RequestParam(defaultValue = "createdAt") String sort,
                             @RequestParam(defaultValue = "desc") String dir,
                             Model model) {

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(direction, sort));

        Page<UrlMapping> linksPage = query.isBlank()
            ? urlService.getAllUrls(pageable)
            : urlService.searchUrls(query, pageable);

        model.addAttribute("linksPage", linksPage);
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("baseUrl", urlService.buildShortUrl(""));

        return "admin/links";
    }

    // ─── Toggle Link Active Status ───────────────────────────────────────────────

    @PostMapping("/links/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLink(@PathVariable Long id) {
        UrlMapping mapping = urlService.getById(id)
            .orElseThrow(() -> new RuntimeException("Link not found"));
        boolean newState = !mapping.isActive();
        urlService.setActive(id, newState);
        log.info("Toggled link ID={} to active={}", id, newState);
        return ResponseEntity.ok(Map.of("active", newState, "message", newState ? "Link enabled" : "Link disabled"));
    }

    // ─── Delete Link ─────────────────────────────────────────────────────────────

    @PostMapping("/links/{id}/delete")
    public String deleteLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        urlService.deleteUrl(id);
        redirectAttributes.addFlashAttribute("successMessage", "Link deleted successfully.");
        return "redirect:/admin/links";
    }

    // ─── Analytics for a single link ─────────────────────────────────────────────

    @GetMapping("/links/{id}/analytics")
    public String linkAnalytics(@PathVariable Long id,
                                 @RequestParam(defaultValue = "30") int days,
                                 Model model) {
        UrlMapping mapping = urlService.getById(id)
            .orElseThrow(() -> new RuntimeException("Link not found: " + id));

        if (!mapping.isAnalyticsEnabled()) {
            model.addAttribute("mapping", mapping);
            model.addAttribute("analyticsDisabled", true);
            model.addAttribute("shortUrl", urlService.buildShortUrl(mapping.getShortCode()));
            return "admin/analytics";
        }

        AnalyticsSummary summary = analyticsService.getAnalytics(id, days);
        model.addAttribute("mapping", mapping);
        model.addAttribute("summary", summary);
        model.addAttribute("days", days);
        model.addAttribute("shortUrl", urlService.buildShortUrl(mapping.getShortCode()));
        model.addAttribute("analyticsDisabled", false);
        return "admin/analytics";
    }

    // ─── Analytics JSON (for chart AJAX refresh) ─────────────────────────────────

    @GetMapping("/links/{id}/analytics/data")
    @ResponseBody
    public ResponseEntity<AnalyticsSummary> analyticsData(@PathVariable Long id,
                                                           @RequestParam(defaultValue = "30") int days) {
        AnalyticsSummary summary = analyticsService.getAnalytics(id, days);
        return ResponseEntity.ok(summary);
    }

    // ─── API Key Management ──────────────────────────────────────────────────────

    @GetMapping("/api-keys")
    public String apiKeys(Model model) {
        List<ApiKey> keys = apiKeyService.listAllKeys();
        model.addAttribute("apiKeys", keys);
        return "admin/api-keys";
    }

    @PostMapping("/api-keys/create")
    public String createApiKey(@RequestParam String label,
                                RedirectAttributes redirectAttributes) {
        if (label == null || label.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Label is required.");
            return "redirect:/admin/api-keys";
        }
        ApiKey newKey = apiKeyService.createApiKey(label.trim());
        // Show the key once immediately after creation
        redirectAttributes.addFlashAttribute("newKeyValue", newKey.getKeyValue());
        redirectAttributes.addFlashAttribute("newKeyLabel", newKey.getLabel());
        return "redirect:/admin/api-keys";
    }

    @PostMapping("/api-keys/{id}/revoke")
    public String revokeApiKey(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        apiKeyService.revokeApiKey(id);
        redirectAttributes.addFlashAttribute("successMessage", "API key revoked.");
        return "redirect:/admin/api-keys";
    }
}
