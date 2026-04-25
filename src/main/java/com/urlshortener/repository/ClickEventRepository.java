package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ClickEvent.DeviceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository for ClickEvent analytics data.
 * Provides aggregated and time-series data for the analytics dashboard.
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    /**
     * Count clicks for a specific URL mapping.
     */
    long countByUrlMappingId(Long urlMappingId);

    /**
     * Count clicks for a URL mapping within a time range.
     */
    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.urlMapping.id = :id AND c.clickedAt BETWEEN :start AND :end")
    long countByUrlMappingIdAndDateRange(
        @Param("id") Long urlMappingId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Get daily click counts for a URL mapping (for sparkline/chart).
     * Returns list of [date_string, count] pairs.
     */
    @Query(value = "SELECT DATE(c.clicked_at) as click_date, COUNT(*) as cnt " +
                   "FROM click_events c WHERE c.url_mapping_id = :id " +
                   "AND c.clicked_at >= :since " +
                   "GROUP BY DATE(c.clicked_at) ORDER BY click_date ASC",
           nativeQuery = true)
    List<Object[]> getDailyClickCounts(@Param("id") Long urlMappingId, @Param("since") LocalDateTime since);

    /**
     * Get browser distribution for a URL mapping.
     * Returns list of [browser_name, count] pairs.
     */
    @Query("SELECT c.browser, COUNT(c) FROM ClickEvent c WHERE c.urlMapping.id = :id GROUP BY c.browser ORDER BY COUNT(c) DESC")
    List<Object[]> getBrowserDistribution(@Param("id") Long urlMappingId);

    /**
     * Get device type distribution for a URL mapping.
     */
    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.urlMapping.id = :id GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> getDeviceDistribution(@Param("id") Long urlMappingId);

    /**
     * Get OS distribution for a URL mapping.
     */
    @Query("SELECT c.operatingSystem, COUNT(c) FROM ClickEvent c WHERE c.urlMapping.id = :id GROUP BY c.operatingSystem ORDER BY COUNT(c) DESC")
    List<Object[]> getOsDistribution(@Param("id") Long urlMappingId);

    /**
     * Get top referrers for a URL mapping.
     */
    @Query("SELECT c.referrer, COUNT(c) FROM ClickEvent c WHERE c.urlMapping.id = :id GROUP BY c.referrer ORDER BY COUNT(c) DESC")
    List<Object[]> getTopReferrers(@Param("id") Long urlMappingId, Pageable pageable);

    /**
     * Get recent clicks for a URL mapping (for activity log).
     */
    @Query("SELECT c FROM ClickEvent c WHERE c.urlMapping.id = :id ORDER BY c.clickedAt DESC")
    List<ClickEvent> findRecentByUrlMappingId(@Param("id") Long urlMappingId, Pageable pageable);

    /**
     * Get total clicks in a given time range (for global stats).
     */
    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.clickedAt BETWEEN :start AND :end")
    long countClicksInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get system-wide daily click trend.
     */
    @Query(value = "SELECT DATE(c.clicked_at) as click_date, COUNT(*) as cnt " +
                   "FROM click_events c WHERE c.clicked_at >= :since " +
                   "GROUP BY DATE(c.clicked_at) ORDER BY click_date ASC",
           nativeQuery = true)
    List<Object[]> getSystemDailyClickCounts(@Param("since") LocalDateTime since);

    /**
     * Delete all click events for a given URL mapping (used when deleting a link).
     */
    @Modifying
    @Query("DELETE FROM ClickEvent c WHERE c.urlMapping.id = :urlMappingId")
    void deleteByUrlMappingId(@Param("urlMappingId") Long urlMappingId);

    /**
     * Count today's global clicks.
     */
    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :startOfDay")
    long countTodayClicks(@Param("startOfDay") LocalDateTime startOfDay);
}
