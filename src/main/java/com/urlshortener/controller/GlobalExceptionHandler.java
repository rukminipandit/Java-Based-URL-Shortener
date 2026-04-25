package com.urlshortener.controller;

import com.urlshortener.exception.Exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler.
 * Returns JSON for /api/** requests and HTML error pages for browser requests.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShortCodeNotFoundException.class)
    public Object handleNotFound(ShortCodeNotFoundException ex, HttpServletRequest request, Model model) {
        log.debug("Short code not found: {}", ex.getMessage());
        if (isApiRequest(request)) return jsonError(ex.getMessage(), HttpStatus.NOT_FOUND);
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", "That short link doesn't exist.");
        return "error/not-found";
    }

    @ExceptionHandler(LinkDisabledException.class)
    public Object handleDisabled(LinkDisabledException ex, HttpServletRequest request, Model model) {
        if (isApiRequest(request)) return jsonError(ex.getMessage(), HttpStatus.GONE);
        model.addAttribute("errorCode", "410");
        model.addAttribute("errorMessage", "This link has been disabled.");
        return "error/not-found";
    }

    @ExceptionHandler(ShortCodeAlreadyExistsException.class)
    public Object handleConflict(ShortCodeAlreadyExistsException ex, HttpServletRequest request) {
        if (isApiRequest(request)) return jsonError(ex.getMessage(), HttpStatus.CONFLICT);
        return "redirect:/?error=code_taken";
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidApiKey(InvalidApiKeyException ex) {
        return jsonError(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public Object handleInvalidPassword(InvalidPasswordException ex, HttpServletRequest request, Model model) {
        if (isApiRequest(request)) return jsonError(ex.getMessage(), HttpStatus.FORBIDDEN);
        model.addAttribute("error", "Incorrect password. Please try again.");
        return "protected-link";
    }

    @ExceptionHandler(InvalidUrlException.class)
    public Object handleInvalidUrl(InvalidUrlException ex, HttpServletRequest request) {
        if (isApiRequest(request)) return jsonError(ex.getMessage(), HttpStatus.BAD_REQUEST);
        return "redirect:/?error=invalid_url";
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
        return jsonError(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        if (isApiRequest(request)) return jsonError("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "Something went wrong. Please try again.");
        return "error/not-found";
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/") ||
               (accept != null && accept.contains("application/json"));
    }

    private ResponseEntity<Map<String, Object>> jsonError(String message, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("error", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
