package com.urlshortener.serviceimpl;

import com.urlshortener.dto.Dtos.*;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AnalyticsService.
 *
 * <p>Persists ClickEvent records and builds AnalyticsSummary DTOs
 * by querying aggregated data from ClickEventRepository.</p>
 *
 * <p>Click recording is intentionally @Async to avoid slowing
 * down the redirect response time.</p>
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private static final List<String> PROXY_HEADERS = Arrays.asList(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_CLIENT_IP",
        "X-Real-IP"
    );

    private final ClickEventRepository clickEventRepository;
    private final UrlMappingRepository urlMappingRepository;
    private final UserAgentParser userAgentParser;

    public AnalyticsServiceImpl(ClickEventRepository clickEventRepository,
                                  UrlMappingRepository urlMappingRepository,
                                  UserAgentParser userAgentParser) {
        this.clickEventRepository = clickEventRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.userAgentParser = userAgentParser;
    }

    // ─── recordClick ─────────────────────────────────────────────────────────────

    @Override
    @Async
    @Transactional
    public void recordClick(Long urlMappingId, String ipAddress, String userAgent, String referrer) {
        try {
            UrlMapping mapping = urlMappingRepository.findById(urlMappingId).orElse(null);
            if (mapping == null) {
                log.warn("Attempted to record click for non-existent mapping ID: {}", urlMappingId);
                return;
            }

            // Parse user agent
            UserAgentParser.ParsedUserAgent parsed = userAgentParser.parse(userAgent);

            // Build click event
            ClickEvent event = new ClickEvent(mapping);
            event.setIpAddress(anonymizeIp(ipAddress));
            event.setUserAgent(truncate(userAgent, 512));
            event.setBrowser(parsed.getBrowser());
            event.setOperatingSystem(parsed.getOs());
            event.setDeviceType(parsed.getDeviceType());
            event.setReferrer(truncate(referrer, 512));

            clickEventRepository.save(event);
            log.debug("Recorded click for mapping ID={}: {} on {}", urlMappingId, parsed.getBrowser(), parsed.getOs());

        } catch (Exception e) {
            // Analytics recording must never break the redirect flow
            log.error("Failed to record click event for mapping ID={}: {}", urlMappingId, e.getMessage());
        }
    }

    // ─── getAnalytics ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummary getAnalytics(Long urlMappingId, int days) {
        UrlMapping mapping = urlMappingRepository.findById(urlMappingId)
            .orElseThrow(() -> new NoSuchElementException("URL mapping not found: " + urlMappingId));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);

        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setUrlMappingId(urlMappingId);
        summary.setShortCode(mapping.getShortCode());
        summary.setTitle(mapping.getTitle());
        summary.setTotalClicks(mapping.getClickCount());

        // Today / this week counts
        summary.setClicksToday(
            clickEventRepository.countByUrlMappingIdAndDateRange(urlMappingId, startOfDay, LocalDateTime.now())
        );
        summary.setClicksThisWeek(
            clickEventRepository.countByUrlMappingIdAndDateRange(urlMappingId, startOfWeek, LocalDateTime.now())
        );

        // Daily trend data
        List<Object[]> dailyRaw = clickEventRepository.getDailyClickCounts(urlMappingId, since);
        List<DailyClickData> dailyData = dailyRaw.stream()
            .map(row -> new DailyClickData(row[0].toString(), ((Number) row[1]).longValue()))
            .collect(Collectors.toList());
        summary.setDailyData(dailyData);

        // Browser breakdown
        summary.setBrowserBreakdown(aggregateToMap(
            clickEventRepository.getBrowserDistribution(urlMappingId)
        ));

        // Device breakdown
        summary.setDeviceBreakdown(aggregateToMap(
            clickEventRepository.getDeviceDistribution(urlMappingId)
        ));

        // OS breakdown
        summary.setOsBreakdown(aggregateToMap(
            clickEventRepository.getOsDistribution(urlMappingId)
        ));

        // Top referrers
        summary.setReferrerBreakdown(aggregateToMap(
            clickEventRepository.getTopReferrers(urlMappingId, PageRequest.of(0, 10))
        ));

        // Recent clicks (last 20)
        List<ClickEvent> recentEvents = clickEventRepository.findRecentByUrlMappingId(
            urlMappingId, PageRequest.of(0, 20)
        );
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        List<RecentClickData> recentClicks = recentEvents.stream().map(event -> {
            RecentClickData rcd = new RecentClickData();
            rcd.setBrowser(event.getBrowser());
            rcd.setOs(event.getOperatingSystem());
            rcd.setDeviceType(event.getDeviceType() != null ? event.getDeviceType().name() : "UNKNOWN");
            rcd.setReferrer(event.getReferrerDisplay());
            rcd.setClickedAt(event.getClickedAt() != null ? event.getClickedAt().format(fmt) : "");
            return rcd;
        }).collect(Collectors.toList());
        summary.setRecentClicks(recentClicks);

        return summary;
    }

    // ─── extractIpAddress ────────────────────────────────────────────────────────

    @Override
    public String extractIpAddress(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For can contain multiple IPs; take the first (client)
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────────

    /**
     * Converts a list of [Object, Number] rows into a sorted LinkedHashMap.
     */
    private Map<String, Long> aggregateToMap(List<Object[]> rows) {
        return rows.stream()
            .filter(row -> row[0] != null)
            .sorted((a, b) -> Long.compare(((Number) b[1]).longValue(), ((Number) a[1]).longValue()))
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> ((Number) row[1]).longValue(),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * Anonymizes an IP address by zeroing the last octet (IPv4) or last group (IPv6).
     */
    private String anonymizeIp(String ip) {
        if (ip == null) return "unknown";
        if (ip.contains(".")) {
            // IPv4: zero the last octet
            int lastDot = ip.lastIndexOf('.');
            return lastDot > 0 ? ip.substring(0, lastDot) + ".0" : ip;
        }
        if (ip.contains(":")) {
            // IPv6: zero the last group
            int lastColon = ip.lastIndexOf(':');
            return lastColon > 0 ? ip.substring(0, lastColon) + ":0" : ip;
        }
        return ip;
    }

    /**
     * Safely truncates a string to a maximum length.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
