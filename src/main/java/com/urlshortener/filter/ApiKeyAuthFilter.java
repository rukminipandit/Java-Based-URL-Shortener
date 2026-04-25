package com.urlshortener.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Filter that intercepts all /api/** requests and validates the API key.
 *
 * <p>The key must be provided in one of:
 * <ul>
 *   <li>HTTP header: {@code X-API-Key: lv_sk_...}</li>
 *   <li>Query parameter: {@code ?apiKey=lv_sk_...}</li>
 * </ul>
 *
 * <p>If the key is missing or invalid, a 401 JSON response is returned
 * immediately and the filter chain is not continued.</p>
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_PARAM  = "apiKey";

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply this filter to /api/** paths
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = extractApiKey(request);

        if (apiKey == null || apiKey.isBlank()) {
            writeUnauthorized(response, "API key is required. Provide it via X-API-Key header or ?apiKey= parameter.");
            return;
        }

        if (!apiKeyService.isValidKey(apiKey)) {
            log.warn("Invalid API key attempt from {}: {}", request.getRemoteAddr(), maskKey(apiKey));
            writeUnauthorized(response, "Invalid or revoked API key.");
            return;
        }

        // Store the validated key in request attribute for controllers to use
        request.setAttribute("validatedApiKey", apiKey);
        filterChain.doFilter(request, response);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────────

    private String extractApiKey(HttpServletRequest request) {
        String fromHeader = request.getHeader(API_KEY_HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) return fromHeader.trim();

        String fromParam = request.getParameter(API_KEY_PARAM);
        if (fromParam != null && !fromParam.isBlank()) return fromParam.trim();

        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
            "success", false,
            "error", message,
            "status", 401
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 6) + "..." + key.substring(key.length() - 3);
    }
}
