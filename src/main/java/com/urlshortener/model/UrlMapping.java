package com.urlshortener.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core entity representing a shortened URL mapping.
 * Holds original URL, short code, optional password hash,
 * analytics toggle, and all associated click events.
 */
@Entity
@Table(
    name = "url_mappings",
    indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_api_key", columnList = "apiKey")
    }
)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Original URL must not be blank")
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @NotBlank(message = "Short code must not be blank")
    @Size(min = 3, max = 30, message = "Short code must be between 3 and 30 characters")
    @Column(name = "short_code", nullable = false, unique = true, length = 30)
    private String shortCode;

    /**
     * Optional title/label for easier identification in the dashboard.
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * BCrypt-hashed password. Null means no password protection.
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /**
     * Whether this link has password protection enabled.
     */
    @Column(name = "password_protected", nullable = false)
    private boolean passwordProtected = false;

    /**
     * Whether click analytics should be tracked for this link.
     */
    @Column(name = "analytics_enabled", nullable = false)
    private boolean analyticsEnabled = true;

    /**
     * Total redirect count. Updated on every successful redirect.
     */
    @Column(name = "click_count", nullable = false)
    private long clickCount = 0L;

    /**
     * Whether this link is active/enabled. Disabled links return 404.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * The API key used to create this link (for ownership tracking).
     */
    @Column(name = "api_key", length = 64)
    private String apiKey;

    /**
     * Last time this short link was clicked.
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * All click events associated with this link (only populated if analyticsEnabled=true).
     */
    @OneToMany(mappedBy = "urlMapping", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("clickedAt DESC")
    private List<ClickEvent> clickEvents = new ArrayList<>();

    // ─── Constructors ───────────────────────────────────────────────────────────

    public UrlMapping() {}

    public UrlMapping(String originalUrl, String shortCode) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
    }

    // ─── Business Logic Methods ─────────────────────────────────────────────────

    /**
     * Increments click count and updates last accessed timestamp.
     * Called on every successful redirect.
     */
    public void recordClick() {
        this.clickCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Returns true if this link requires a password before redirecting.
     */
    public boolean requiresPassword() {
        return this.passwordProtected && this.passwordHash != null && !this.passwordHash.isBlank();
    }

    /**
     * Returns a short preview of the original URL for display purposes.
     */
    public String getOriginalUrlPreview() {
        if (originalUrl == null) return "";
        return originalUrl.length() > 60 ? originalUrl.substring(0, 60) + "..." : originalUrl;
    }

    /**
     * Extracts the domain from the original URL for display.
     */
    public String getDomain() {
        if (originalUrl == null) return "";
        try {
            String url = originalUrl;
            if (url.startsWith("http://")) url = url.substring(7);
            else if (url.startsWith("https://")) url = url.substring(8);
            int slashIdx = url.indexOf('/');
            return slashIdx > 0 ? url.substring(0, slashIdx) : url;
        } catch (Exception e) {
            return originalUrl;
        }
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isPasswordProtected() { return passwordProtected; }
    public void setPasswordProtected(boolean passwordProtected) { this.passwordProtected = passwordProtected; }

    public boolean isAnalyticsEnabled() { return analyticsEnabled; }
    public void setAnalyticsEnabled(boolean analyticsEnabled) { this.analyticsEnabled = analyticsEnabled; }

    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ClickEvent> getClickEvents() { return clickEvents; }
    public void setClickEvents(List<ClickEvent> clickEvents) { this.clickEvents = clickEvents; }

    @Override
    public String toString() {
        return "UrlMapping{id=" + id + ", shortCode='" + shortCode + "', clickCount=" + clickCount + "}";
    }
}
