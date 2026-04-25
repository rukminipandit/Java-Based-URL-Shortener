package com.urlshortener.controller;

import com.urlshortener.dto.Dtos.CreateUrlRequest;
import com.urlshortener.dto.Dtos.CreateUrlResponse;
import com.urlshortener.exception.Exceptions.InvalidPasswordException;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the public-facing pages:
 * <ul>
 *   <li>GET  /              — homepage with the URL shortening form</li>
 *   <li>POST /shorten       — creates a short URL (form submission)</li>
 *   <li>GET  /{shortCode}   — resolves and redirects (or shows password form)</li>
 *   <li>POST /protected/{shortCode} — password verification for protected links</li>
 * </ul>
 */
@Controller
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);

    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    public UrlController(UrlService urlService, AnalyticsService analyticsService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
    }

    // ─── Homepage ────────────────────────────────────────────────────────────────

    @GetMapping("/")
    public String homepage(Model model,
                           @RequestParam(required = false) String error,
                           @RequestParam(required = false) String shortUrl,
                           @RequestParam(required = false) String shortCode) {
        model.addAttribute("createRequest", new CreateUrlRequest());
        model.addAttribute("error", error);
        model.addAttribute("shortUrl", shortUrl);
        model.addAttribute("shortCode", shortCode);
        return "index";
    }

    // ─── Shorten (form POST) ─────────────────────────────────────────────────────

    @PostMapping("/shorten")
    public String shortenUrl(@Valid @ModelAttribute("createRequest") CreateUrlRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "index";
        }

        try {
            CreateUrlResponse response = urlService.createShortUrl(request);
            redirectAttributes.addFlashAttribute("successUrl", response.getShortUrl());
            redirectAttributes.addFlashAttribute("successCode", response.getShortCode());
            redirectAttributes.addFlashAttribute("analyticsEnabled", response.isAnalyticsEnabled());
            redirectAttributes.addFlashAttribute("passwordProtected", response.isPasswordProtected());
            return "redirect:/";
        } catch (Exception ex) {
            model.addAttribute("createRequest", request);
            model.addAttribute("formError", ex.getMessage());
            return "index";
        }
    }

    // ─── AJAX Shorten ────────────────────────────────────────────────────────────

    @PostMapping("/api/shorten/web")
    @ResponseBody
    public ResponseEntity<CreateUrlResponse> shortenAjax(@RequestBody CreateUrlRequest request) {
        // Public web endpoint (no API key required for the homepage form via AJAX)
        request.setApiKey(null);
        CreateUrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity.ok(response);
    }

    // ─── Redirect ────────────────────────────────────────────────────────────────

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]{3,30}}")
    public String redirect(@PathVariable String shortCode,
                           @RequestParam(required = false) String pwd,
                           Model model,
                           HttpServletRequest request) {

        UrlMapping mapping = urlService.resolve(shortCode);

        // If password-protected and no password provided, show the password entry form
        if (mapping.requiresPassword() && (pwd == null || pwd.isBlank())) {
            model.addAttribute("shortCode", shortCode);
            model.addAttribute("domain", mapping.getDomain());
            model.addAttribute("error", null);
            return "protected-link";
        }

        // Attempt to resolve and redirect
        String ipAddress  = analyticsService.extractIpAddress(request);
        String userAgent  = request.getHeader("User-Agent");
        String referrer   = request.getHeader("Referer");

        String destination = urlService.resolveAndRecord(shortCode, pwd, ipAddress, userAgent, referrer);

        if (destination == null) {
            // Password required but not supplied
            model.addAttribute("shortCode", shortCode);
            model.addAttribute("domain", mapping.getDomain());
            model.addAttribute("error", null);
            return "protected-link";
        }

        log.debug("Redirecting {} -> {}", shortCode, destination);
        return "redirect:" + destination;
    }

    // ─── Password Verification ───────────────────────────────────────────────────

    @PostMapping("/protected/{shortCode}")
    public String verifyPassword(@PathVariable String shortCode,
                                  @RequestParam String password,
                                  Model model,
                                  HttpServletRequest request) {
        try {
            String ipAddress = analyticsService.extractIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String referrer  = request.getHeader("Referer");

            String destination = urlService.resolveAndRecord(shortCode, password, ipAddress, userAgent, referrer);

            if (destination == null) {
                model.addAttribute("shortCode", shortCode);
                model.addAttribute("error", "Password is required.");
                return "protected-link";
            }

            return "redirect:" + destination;

        } catch (InvalidPasswordException ex) {
            UrlMapping mapping = urlService.resolve(shortCode);
            model.addAttribute("shortCode", shortCode);
            model.addAttribute("domain", mapping.getDomain());
            model.addAttribute("error", "Incorrect password. Please try again.");
            return "protected-link";
        }
    }

    // ─── Check availability (AJAX) ───────────────────────────────────────────────

    @GetMapping("/check-code")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> checkCodeAvailability(
            @RequestParam String code) {
        boolean available = urlService.isShortCodeAvailable(code);
        return ResponseEntity.ok(java.util.Map.of(
            "code", code,
            "available", available,
            "message", available ? "✓ Available" : "✗ Already taken"
        ));
    }
}
