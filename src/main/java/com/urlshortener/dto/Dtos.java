package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * All Data Transfer Objects for the URL shortener application.
 * Kept in one file for clarity; split into separate files if needed.
 */
public final class Dtos {

    private Dtos() {}

    // ─── Request DTOs ────────────────────────────────────────────────────────────

    /**
     * Request DTO for creating a new short URL.
     */
    public static class CreateUrlRequest {

        @NotBlank(message = "URL is required")
        private String originalUrl;

@Pattern(regexp = "^([a-zA-Z0-9_-]{3,30})?$", message = "Only letters, numbers, hyphens, and underscores allowed")
private String customCode;
        private String title;
        private boolean analyticsEnabled = true;
        private boolean passwordProtected = false;
        private String password;
        private String apiKey;  

        public CreateUrlRequest() {}

        public String getOriginalUrl() { return originalUrl; }
        public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
        public String getCustomCode() { return customCode; }
        public void setCustomCode(String customCode) { this.customCode = customCode; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public boolean isAnalyticsEnabled() { return analyticsEnabled; }
        public void setAnalyticsEnabled(boolean analyticsEnabled) { this.analyticsEnabled = analyticsEnabled; }
        public boolean isPasswordProtected() { return passwordProtected; }
        public void setPasswordProtected(boolean passwordProtected) { this.passwordProtected = passwordProtected; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    /**
     * Response DTO after successfully creating a short URL.
     */
    public static class CreateUrlResponse {
        private boolean success;
        private String shortUrl;
        private String shortCode;
        private String originalUrl;
        private boolean analyticsEnabled;
        private boolean passwordProtected;
        private String message;
        private String error;

        public static CreateUrlResponse success(String shortUrl, String shortCode, String originalUrl,
                                                 boolean analyticsEnabled, boolean passwordProtected) {
            CreateUrlResponse r = new CreateUrlResponse();
            r.success = true;
            r.shortUrl = shortUrl;
            r.shortCode = shortCode;
            r.originalUrl = originalUrl;
            r.analyticsEnabled = analyticsEnabled;
            r.passwordProtected = passwordProtected;
            r.message = "Short URL created successfully";
            return r;
        }

        public static CreateUrlResponse error(String errorMessage) {
            CreateUrlResponse r = new CreateUrlResponse();
            r.success = false;
            r.error = errorMessage;
            return r;
        }

        public boolean isSuccess() { return success; }
        public String getShortUrl() { return shortUrl; }
        public String getShortCode() { return shortCode; }
        public String getOriginalUrl() { return originalUrl; }
        public boolean isAnalyticsEnabled() { return analyticsEnabled; }
        public boolean isPasswordProtected() { return passwordProtected; }
        public String getMessage() { return message; }
        public String getError() { return error; }
    }

    /**
     * Analytics summary for a single URL.
     */
    public static class AnalyticsSummary {
        private Long urlMappingId;
        private String shortCode;
        private String title;
        private long totalClicks;
        private long clicksToday;
        private long clicksThisWeek;
        private List<DailyClickData> dailyData;
        private Map<String, Long> browserBreakdown;
        private Map<String, Long> deviceBreakdown;
        private Map<String, Long> osBreakdown;
        private Map<String, Long> referrerBreakdown;
        private List<RecentClickData> recentClicks;

        public AnalyticsSummary() {}

        public Long getUrlMappingId() { return urlMappingId; }
        public void setUrlMappingId(Long urlMappingId) { this.urlMappingId = urlMappingId; }
        public String getShortCode() { return shortCode; }
        public void setShortCode(String shortCode) { this.shortCode = shortCode; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public long getTotalClicks() { return totalClicks; }
        public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }
        public long getClicksToday() { return clicksToday; }
        public void setClicksToday(long clicksToday) { this.clicksToday = clicksToday; }
        public long getClicksThisWeek() { return clicksThisWeek; }
        public void setClicksThisWeek(long clicksThisWeek) { this.clicksThisWeek = clicksThisWeek; }
        public List<DailyClickData> getDailyData() { return dailyData; }
        public void setDailyData(List<DailyClickData> dailyData) { this.dailyData = dailyData; }
        public Map<String, Long> getBrowserBreakdown() { return browserBreakdown; }
        public void setBrowserBreakdown(Map<String, Long> browserBreakdown) { this.browserBreakdown = browserBreakdown; }
        public Map<String, Long> getDeviceBreakdown() { return deviceBreakdown; }
        public void setDeviceBreakdown(Map<String, Long> deviceBreakdown) { this.deviceBreakdown = deviceBreakdown; }
        public Map<String, Long> getOsBreakdown() { return osBreakdown; }
        public void setOsBreakdown(Map<String, Long> osBreakdown) { this.osBreakdown = osBreakdown; }
        public Map<String, Long> getReferrerBreakdown() { return referrerBreakdown; }
        public void setReferrerBreakdown(Map<String, Long> referrerBreakdown) { this.referrerBreakdown = referrerBreakdown; }
        public List<RecentClickData> getRecentClicks() { return recentClicks; }
        public void setRecentClicks(List<RecentClickData> recentClicks) { this.recentClicks = recentClicks; }
    }

    /**
     * Single day's click count for chart rendering.
     */
    public static class DailyClickData {
        private String date;
        private long count;

        public DailyClickData(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() { return date; }
        public long getCount() { return count; }
    }

    /**
     * Single recent click entry for the activity log.
     */
    public static class RecentClickData {
        private String browser;
        private String os;
        private String deviceType;
        private String referrer;
        private String clickedAt;

        public RecentClickData() {}

        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }
        public String getOs() { return os; }
        public void setOs(String os) { this.os = os; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getReferrer() { return referrer; }
        public void setReferrer(String referrer) { this.referrer = referrer; }
        public String getClickedAt() { return clickedAt; }
        public void setClickedAt(String clickedAt) { this.clickedAt = clickedAt; }
    }

    /**
     * Global dashboard statistics.
     */
    public static class DashboardStats {
        private long totalLinks;
        private long activeLinks;
        private long totalClicks;
        private long clicksToday;
        private long protectedLinks;
        private long analyticsLinks;
        private List<Object> topLinks;
        private List<DailyClickData> last7DaysTrend;

        public DashboardStats() {}

        public long getTotalLinks() { return totalLinks; }
        public void setTotalLinks(long totalLinks) { this.totalLinks = totalLinks; }
        public long getActiveLinks() { return activeLinks; }
        public void setActiveLinks(long activeLinks) { this.activeLinks = activeLinks; }
        public long getTotalClicks() { return totalClicks; }
        public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }
        public long getClicksToday() { return clicksToday; }
        public void setClicksToday(long clicksToday) { this.clicksToday = clicksToday; }
        public long getProtectedLinks() { return protectedLinks; }
        public void setProtectedLinks(long protectedLinks) { this.protectedLinks = protectedLinks; }
        public long getAnalyticsLinks() { return analyticsLinks; }
        public void setAnalyticsLinks(long analyticsLinks) { this.analyticsLinks = analyticsLinks; }
        public List<Object> getTopLinks() { return topLinks; }
        public void setTopLinks(List<Object> topLinks) { this.topLinks = topLinks; }
        public List<DailyClickData> getLast7DaysTrend() { return last7DaysTrend; }
        public void setLast7DaysTrend(List<DailyClickData> last7DaysTrend) { this.last7DaysTrend = last7DaysTrend; }
    }
}
