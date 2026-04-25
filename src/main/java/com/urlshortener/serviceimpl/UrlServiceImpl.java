package com.urlshortener.serviceimpl;

import com.urlshortener.dto.Dtos.*;
import com.urlshortener.exception.Exceptions.*;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.ApiKeyService;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.ShortCodeGenerator;
import com.urlshortener.util.UrlValidator;
import com.urlshortener.util.UrlValidator.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Primary implementation of UrlService.
 *
 * <p>Handles all URL shortening business logic including:
 * <ul>
 *   <li>URL validation and normalization via UrlValidator</li>
 *   <li>Short code generation (random or custom) via ShortCodeGenerator</li>
 *   <li>Password BCrypt hashing via Spring PasswordEncoder</li>
 *   <li>Click recording and delegation to AnalyticsService</li>
 *   <li>Transactional operations against UrlMappingRepository</li>
 * </ul>
 * </p>
 */
@Service
@Transactional
public class UrlServiceImpl implements UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);
    private static final int MAX_CODE_GENERATION_RETRIES = 10;

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;
    private final AnalyticsService analyticsService;
    private final ApiKeyService apiKeyService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlValidator urlValidator;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlServiceImpl(
            UrlMappingRepository urlMappingRepository,
            ClickEventRepository clickEventRepository,
            AnalyticsService analyticsService,
            ApiKeyService apiKeyService,
            ShortCodeGenerator shortCodeGenerator,
            UrlValidator urlValidator,
            PasswordEncoder passwordEncoder) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickEventRepository = clickEventRepository;
        this.analyticsService = analyticsService;
        this.apiKeyService = apiKeyService;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlValidator = urlValidator;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── createShortUrl ──────────────────────────────────────────────────────────

    @Override
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        log.debug("Creating short URL for: {}", request.getOriginalUrl());

        // 1. Validate API key if provided
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            if (!apiKeyService.isValidKey(request.getApiKey())) {
                throw new InvalidApiKeyException();
            }
        }

        // 2. Validate and normalize the original URL
        ValidationResult validation = urlValidator.validate(request.getOriginalUrl());
        if (!validation.isValid()) {
            throw new InvalidUrlException(validation.getError());
        }
        String normalizedUrl = validation.getNormalizedUrl();

        // 3. Determine short code: custom or auto-generated
        String shortCode = resolveShortCode(request);

        // 4. Build and populate the entity
        UrlMapping mapping = new UrlMapping(normalizedUrl, shortCode);
        mapping.setTitle(request.getTitle() != null ? request.getTitle().trim() : extractTitleFromUrl(normalizedUrl));
        mapping.setAnalyticsEnabled(request.isAnalyticsEnabled());
        mapping.setApiKey(request.getApiKey());

        // 5. Handle password protection
        if (request.isPasswordProtected() && request.getPassword() != null && !request.getPassword().isBlank()) {
            mapping.setPasswordProtected(true);
            mapping.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            log.debug("Password protection enabled for short code: {}", shortCode);
        } else {
            mapping.setPasswordProtected(false);
        }

        // 6. Persist
        UrlMapping saved = urlMappingRepository.save(mapping);
        log.info("Created short URL: {} -> {}", shortCode, normalizedUrl);

        return CreateUrlResponse.success(
            buildShortUrl(shortCode),
            shortCode,
            normalizedUrl,
            saved.isAnalyticsEnabled(),
            saved.isPasswordProtected()
        );
    }

    // ─── resolve ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UrlMapping resolve(String shortCode) {
        UrlMapping mapping = urlMappingRepository
            .findByShortCodeIgnoreCase(shortCode)
            .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        if (!mapping.isActive()) {
            throw new LinkDisabledException(shortCode);
        }

        return mapping;
    }

    // ─── resolveAndRecord ────────────────────────────────────────────────────────

    @Override
    public String resolveAndRecord(String shortCode, String password, String ipAddress,
                                    String userAgent, String referrer) {
        UrlMapping mapping = resolve(shortCode);

        // Verify password if required
        if (mapping.requiresPassword()) {
            if (password == null || password.isBlank()) {
                // Signal that a password is needed (don't throw InvalidPassword — let controller show form)
                return null;
            }
            if (!passwordEncoder.matches(password, mapping.getPasswordHash())) {
                throw new InvalidPasswordException();
            }
        }

        // Record click atomically in DB
        urlMappingRepository.incrementClickCount(mapping.getId(), LocalDateTime.now());

        // Delegate full analytics recording if enabled
        if (mapping.isAnalyticsEnabled()) {
            analyticsService.recordClick(mapping.getId(), ipAddress, userAgent, referrer);
        }

        log.debug("Redirect: {} -> {}", shortCode, mapping.getOriginalUrl());
        return mapping.getOriginalUrl();
    }

    // ─── isShortCodeAvailable ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isShortCodeAvailable(String shortCode) {
        return !urlMappingRepository.existsByShortCodeIgnoreCase(shortCode);
    }

    // ─── getAllUrls ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<UrlMapping> getAllUrls(Pageable pageable) {
        return urlMappingRepository.findAll(pageable);
    }

    // ─── searchUrls ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<UrlMapping> searchUrls(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getAllUrls(pageable);
        }
        return urlMappingRepository.searchByQuery(query.trim(), pageable);
    }

    // ─── getById ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<UrlMapping> getById(Long id) {
        return urlMappingRepository.findById(id);
    }

    // ─── setActive ───────────────────────────────────────────────────────────────

    @Override
    public void setActive(Long id, boolean active) {
        UrlMapping mapping = urlMappingRepository.findById(id)
            .orElseThrow(() -> new ShortCodeNotFoundException("ID: " + id));
        mapping.setActive(active);
        urlMappingRepository.save(mapping);
        log.info("Set active={} for URL mapping ID={}", active, id);
    }

    // ─── deleteUrl ───────────────────────────────────────────────────────────────

    @Override
    public void deleteUrl(Long id) {
        UrlMapping mapping = urlMappingRepository.findById(id)
            .orElseThrow(() -> new ShortCodeNotFoundException("ID: " + id));
        clickEventRepository.deleteByUrlMappingId(id);
        urlMappingRepository.delete(mapping);
        log.info("Deleted URL mapping ID={}", id);
    }

    // ─── buildShortUrl ───────────────────────────────────────────────────────────

    @Override
    public String buildShortUrl(String shortCode) {
        return baseUrl.stripTrailing() + "/" + shortCode;
    }

    // ─── getDashboardStats ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalLinks(urlMappingRepository.count());
        stats.setActiveLinks(urlMappingRepository.countByActiveTrue());
        stats.setTotalClicks(urlMappingRepository.sumAllClickCounts());
        stats.setProtectedLinks(urlMappingRepository.countByPasswordProtectedTrue());
        stats.setAnalyticsLinks(urlMappingRepository.countByAnalyticsEnabledTrue());

        // Today's clicks
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        stats.setClicksToday(clickEventRepository.countTodayClicks(startOfDay));

        // Top 5 links by click count
        List<UrlMapping> topLinks = urlMappingRepository.findTopByClickCount(PageRequest.of(0, 5));
        stats.setTopLinks(topLinks.stream().map(m -> (Object) m).collect(Collectors.toList()));

        // Last 7 days trend
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> rawTrend = clickEventRepository.getSystemDailyClickCounts(sevenDaysAgo);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        List<DailyClickData> trend = rawTrend.stream()
            .map(row -> new DailyClickData(row[0].toString(), ((Number) row[1]).longValue()))
            .collect(Collectors.toList());
        stats.setLast7DaysTrend(trend);

        return stats;
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────────

    /**
     * Resolves the short code to use: custom (if provided and valid) or auto-generated.
     */
    private String resolveShortCode(CreateUrlRequest request) {
        String customCode = request.getCustomCode();
        if (customCode != null && !customCode.isBlank()) {
            String sanitized = shortCodeGenerator.sanitizeCode(customCode);
            if (!shortCodeGenerator.isValidCustomCode(sanitized)) {
                throw new InvalidUrlException("Custom code contains invalid characters");
            }
            if (!isShortCodeAvailable(sanitized)) {
                throw new ShortCodeAlreadyExistsException(sanitized);
            }
            return sanitized;
        }

        // Auto-generate with collision retry
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_RETRIES; attempt++) {
            String generated = shortCodeGenerator.generateRandom();
            if (isShortCodeAvailable(generated)) {
                return generated;
            }
            log.debug("Short code collision on attempt {}: {}", attempt + 1, generated);
        }

        throw new RuntimeException("Failed to generate a unique short code after " + MAX_CODE_GENERATION_RETRIES + " attempts");
    }

    /**
     * Extracts a readable title hint from a URL (uses the domain as fallback title).
     */
    private String extractTitleFromUrl(String url) {
        try {
            String cleaned = url.replaceFirst("https?://", "").replaceFirst("www\\.", "");
            int slash = cleaned.indexOf('/');
            return slash > 0 ? cleaned.substring(0, slash) : cleaned;
        } catch (Exception e) {
            return url;
        }
    }
}
