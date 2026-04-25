package com.urlshortener.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents an API key that grants permission to create/manage shortened URLs
 * via the REST API. Each key has a label, rate limit, and active status.
 */
@Entity
@Table(
    name = "api_keys",
    indexes = {
        @Index(name = "idx_api_key_value", columnList = "keyValue", unique = true)
    }
)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable label for identifying the key (e.g., "My App", "Marketing Bot").
     */
    @Column(name = "label", nullable = false, length = 128)
    private String label;

    /**
     * The actual API key value prefixed with "lv_sk_".
     */
    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    /**
     * Whether this key is active and usable.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Total number of URLs created with this key.
     */
    @Column(name = "usage_count", nullable = false)
    private long usageCount = 0L;

    /**
     * Max URLs this key can create per day. -1 = unlimited.
     */
    @Column(name = "daily_limit", nullable = false)
    private int dailyLimit = -1;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── Constructors ───────────────────────────────────────────────────────────

    public ApiKey() {}

    public ApiKey(String label, String keyValue) {
        this.label = label;
        this.keyValue = keyValue;
    }

    // ─── Business Logic ─────────────────────────────────────────────────────────

    /**
     * Records a usage of this API key.
     */
    public void recordUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Returns a masked version of the key for display (e.g., "lv_sk_xxxx...abcd").
     */
    public String getMaskedKey() {
        if (keyValue == null || keyValue.length() < 12) return "****";
        return keyValue.substring(0, 8) + "..." + keyValue.substring(keyValue.length() - 4);
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }

    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
