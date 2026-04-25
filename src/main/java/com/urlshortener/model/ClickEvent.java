package com.urlshortener.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single click/redirect event on a shortened URL.
 * Captured only when analyticsEnabled=true on the parent UrlMapping.
 * Stores device info, browser, OS, referrer, and IP for analytics.
 */
@Entity
@Table(
    name = "click_events",
    indexes = {
        @Index(name = "idx_click_url_mapping", columnList = "url_mapping_id"),
        @Index(name = "idx_click_clicked_at", columnList = "clickedAt"),
        @Index(name = "idx_click_country", columnList = "country")
    }
)
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    /**
     * Hashed/anonymized IP address of the visitor.
     */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /**
     * Raw User-Agent string from the HTTP request.
     */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * Parsed browser name (e.g., "Chrome", "Firefox", "Safari").
     */
    @Column(name = "browser", length = 64)
    private String browser;

    /**
     * Parsed operating system (e.g., "Windows 10", "iOS", "Android").
     */
    @Column(name = "operating_system", length = 64)
    private String operatingSystem;

    /**
     * Device type: DESKTOP, MOBILE, TABLET, or UNKNOWN.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 16)
    private DeviceType deviceType = DeviceType.UNKNOWN;

    /**
     * HTTP Referer header — where the user came from.
     */
    @Column(name = "referrer", length = 512)
    private String referrer;

    /**
     * Country derived from IP (if geo-lookup is enabled).
     */
    @Column(name = "country", length = 64)
    private String country;

    @CreationTimestamp
    @Column(name = "clickedAt", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    // ─── Enum ──────────────────────────────────────────────────────────────────

    public enum DeviceType {
        DESKTOP, MOBILE, TABLET, UNKNOWN;

        /**
         * Parses device type from a User-Agent string.
         */
        public static DeviceType fromUserAgent(String ua) {
            if (ua == null || ua.isBlank()) return UNKNOWN;
            String lower = ua.toLowerCase();
            if (lower.contains("tablet") || lower.contains("ipad")) return TABLET;
            if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) return MOBILE;
            return DESKTOP;
        }
    }

    // ─── Constructors ───────────────────────────────────────────────────────────

    public ClickEvent() {}

    public ClickEvent(UrlMapping urlMapping) {
        this.urlMapping = urlMapping;
    }

    // ─── Business Logic ─────────────────────────────────────────────────────────

    /**
     * Returns a simplified referrer domain or "Direct" if no referrer.
     */
    public String getReferrerDisplay() {
        if (referrer == null || referrer.isBlank()) return "Direct";
        try {
            String r = referrer;
            if (r.startsWith("http://")) r = r.substring(7);
            else if (r.startsWith("https://")) r = r.substring(8);
            int slash = r.indexOf('/');
            return slash > 0 ? r.substring(0, slash) : r;
        } catch (Exception e) {
            return referrer.length() > 40 ? referrer.substring(0, 40) + "..." : referrer;
        }
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UrlMapping getUrlMapping() { return urlMapping; }
    public void setUrlMapping(UrlMapping urlMapping) { this.urlMapping = urlMapping; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDateTime getClickedAt() { return clickedAt; }
    public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }

    @Override
    public String toString() {
        return "ClickEvent{id=" + id + ", browser='" + browser + "', device=" + deviceType + ", clickedAt=" + clickedAt + "}";
    }
}
