package com.urlshortener.util;

import com.urlshortener.model.ClickEvent.DeviceType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Parses HTTP User-Agent strings to extract browser, OS, and device type.
 *
 * <p>Uses regex patterns rather than external libraries to avoid dependency issues.
 * The parsing is intentionally lightweight — suitable for analytics bucketing,
 * not for precise UA fingerprinting.</p>
 */
@Component
public class UserAgentParser {

    // ─── Browser Patterns ──────────────────────────────────────────────────────

    private static final Pattern EDGE      = Pattern.compile("Edg[eA]?/([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHROME    = Pattern.compile("Chrome/([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAFARI    = Pattern.compile("Safari/([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIREFOX   = Pattern.compile("Firefox/([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPERA     = Pattern.compile("OPR/([\\d.]+)|Opera/([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IE        = Pattern.compile("MSIE ([\\d.]+)|Trident.*rv:([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAMSUNG   = Pattern.compile("SamsungBrowser/([\\d.]+)", Pattern.CASE_INSENSITIVE);

    // ─── OS Patterns ────────────────────────────────────────────────────────────

    private static final Pattern WINDOWS   = Pattern.compile("Windows NT ([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MACOS     = Pattern.compile("Mac OS X ([\\d_.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANDROID   = Pattern.compile("Android ([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IOS       = Pattern.compile("OS ([\\d_]+) like Mac OS", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINUX     = Pattern.compile("Linux", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHROMEOS  = Pattern.compile("CrOS", Pattern.CASE_INSENSITIVE);

    // ─── Device Patterns ────────────────────────────────────────────────────────

    private static final Pattern TABLET    = Pattern.compile("iPad|tablet|Kindle|PlayBook|KFAPWI", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOBILE    = Pattern.compile("iPhone|Android.*Mobile|Windows Phone|BlackBerry|BB|webOS|IEMobile|Mobile", Pattern.CASE_INSENSITIVE);

    /**
     * Parses the browser name from a User-Agent string.
     *
     * @param userAgent the raw User-Agent string
     * @return human-readable browser name, or "Unknown Browser" if not detected
     */
    public String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown Browser";

        if (SAMSUNG.matcher(userAgent).find()) return "Samsung Browser";
        if (EDGE.matcher(userAgent).find())    return "Microsoft Edge";
        if (IE.matcher(userAgent).find())      return "Internet Explorer";
        if (OPERA.matcher(userAgent).find())   return "Opera";
        if (FIREFOX.matcher(userAgent).find()) return "Firefox";
        // Chrome must come before Safari since Chrome UA contains "Safari"
        if (CHROME.matcher(userAgent).find())  return "Chrome";
        if (SAFARI.matcher(userAgent).find())  return "Safari";

        return "Other Browser";
    }

    /**
     * Parses the operating system from a User-Agent string.
     *
     * @param userAgent the raw User-Agent string
     * @return human-readable OS name, or "Unknown OS" if not detected
     */
    public String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown OS";

        var androidMatcher = ANDROID.matcher(userAgent);
        if (androidMatcher.find()) return "Android " + androidMatcher.group(1);

        var iosMatcher = IOS.matcher(userAgent);
        if (iosMatcher.find()) return "iOS " + iosMatcher.group(1).replace("_", ".");

        var windowsMatcher = WINDOWS.matcher(userAgent);
        if (windowsMatcher.find()) {
            return "Windows " + mapWindowsNt(windowsMatcher.group(1));
        }

        var macosMatcher = MACOS.matcher(userAgent);
        if (macosMatcher.find()) return "macOS " + macosMatcher.group(1).replace("_", ".");

        if (CHROMEOS.matcher(userAgent).find()) return "ChromeOS";
        if (LINUX.matcher(userAgent).find())    return "Linux";

        return "Other OS";
    }

    /**
     * Determines device type (DESKTOP, MOBILE, TABLET) from User-Agent.
     *
     * @param userAgent the raw User-Agent string
     * @return DeviceType enum value
     */
    public DeviceType parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return DeviceType.UNKNOWN;
        if (TABLET.matcher(userAgent).find()) return DeviceType.TABLET;
        if (MOBILE.matcher(userAgent).find()) return DeviceType.MOBILE;
        return DeviceType.DESKTOP;
    }

    /**
     * Parses all device info from a User-Agent string in one call.
     *
     * @param userAgent the raw User-Agent string
     * @return a ParsedUserAgent containing browser, OS, and device type
     */
    public ParsedUserAgent parse(String userAgent) {
        return new ParsedUserAgent(
            parseBrowser(userAgent),
            parseOs(userAgent),
            parseDeviceType(userAgent)
        );
    }

    /**
     * Maps Windows NT version numbers to marketing names.
     */
    private String mapWindowsNt(String ntVersion) {
        return switch (ntVersion) {
            case "10.0" -> "10/11";
            case "6.3"  -> "8.1";
            case "6.2"  -> "8";
            case "6.1"  -> "7";
            case "6.0"  -> "Vista";
            case "5.1"  -> "XP";
            default     -> ntVersion;
        };
    }

    // ─── Result Class ────────────────────────────────────────────────────────────

    public static class ParsedUserAgent {
        private final String browser;
        private final String os;
        private final DeviceType deviceType;

        public ParsedUserAgent(String browser, String os, DeviceType deviceType) {
            this.browser = browser;
            this.os = os;
            this.deviceType = deviceType;
        }

        public String getBrowser() { return browser; }
        public String getOs() { return os; }
        public DeviceType getDeviceType() { return deviceType; }

        @Override
        public String toString() {
            return "ParsedUserAgent{browser='" + browser + "', os='" + os + "', device=" + deviceType + "}";
        }
    }
}
