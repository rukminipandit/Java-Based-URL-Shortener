# LinkVault — URL Shortener

A self-hosted, feature-rich URL shortener built with **Spring Boot 3** and **MySQL**. LinkVault gives you branded short links, password protection, per-link analytics, an admin dashboard, and a REST API — all deployable on your own infrastructure.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Admin Dashboard](#admin-dashboard)
- [Security Model](#security-model)
- [Data Model](#data-model)
- [Error Handling](#error-handling)
- [Production Checklist](#production-checklist)

---

## Features

**Link Management**
- Shorten any URL with a randomly generated or custom short code (3–30 alphanumeric characters, hyphens, and underscores)
- Optional title/label for easy identification in the dashboard
- Enable or disable individual links without deleting them
- Real-time short code availability check via AJAX

**Password Protection**
- Protect any link with a BCrypt-hashed password
- Visitors are shown a password entry page before being redirected
- Wrong password returns a clear error; correct password redirects instantly

**Analytics**
- Per-link toggle — opt out of tracking on sensitive links
- Every click event records: browser, OS, device type (Desktop / Mobile / Tablet), referrer domain, and hashed IP address
- Queryable by time window (1–365 days)
- Aggregated breakdowns by browser, OS, device, and referrer
- Daily click trend data for charting
- Recent activity log with timestamps

**Admin Dashboard**
- Overview stats: total links, active links, total clicks, clicks today, protected links, analytics-enabled links
- Full link management table with pagination, sort, and toggle controls
- Per-link analytics drilldown page
- API key management UI
- Protected by Spring Security form login (session-based)

**REST API**
- Full CRUD on links via `/api/v1`
- API key authentication via `X-API-Key` header or `?apiKey=` query param
- Paginated link listing with sort direction control
- Analytics endpoint per link
- Global stats endpoint
- Short code availability check

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Web | Spring MVC + Thymeleaf |
| Security | Spring Security 6 |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8+ |
| Build | Maven |
| UA Parsing | UserAgentUtils 1.21 |
| Utilities | Guava 32, Apache Commons Lang 3 |

---

## Project Structure

```
urlshortener/
├── src/main/java/com/urlshortener/
│   ├── UrlShortenerApplication.java     # Entry point
│   ├── config/
│   │   ├── AppConfig.java               # Bean definitions (ShortCodeGenerator, UrlValidator, etc.)
│   │   └── SecurityConfig.java          # Spring Security — public, admin, and API routes
│   ├── controller/
│   │   ├── UrlController.java           # Public pages: homepage, redirect, password entry
│   │   ├── ApiController.java           # REST API — /api/v1/**
│   │   ├── DashboardController.java     # Admin dashboard pages
│   │   └── GlobalExceptionHandler.java  # Centralized error responses
│   ├── filter/
│   │   └── ApiKeyAuthFilter.java        # Validates X-API-Key before API requests
│   ├── model/
│   │   ├── UrlMapping.java              # Core entity: short code, original URL, flags
│   │   ├── ClickEvent.java              # Analytics entity: one row per redirect
│   │   └── ApiKey.java                  # API key entity
│   ├── repository/
│   │   ├── UrlMappingRepository.java
│   │   ├── ClickEventRepository.java
│   │   └── ApiKeyRepository.java
│   ├── service/                         # Interfaces
│   │   ├── UrlService.java
│   │   ├── AnalyticsService.java
│   │   └── ApiKeyService.java
│   ├── serviceimpl/                     # Implementations
│   │   ├── UrlServiceImpl.java
│   │   ├── AnalyticsServiceImpl.java
│   │   └── ApiKeyServiceImpl.java
│   ├── dto/
│   │   └── Dtos.java                    # All request/response DTOs in one file
│   ├── exception/
│   │   └── Exceptions.java              # All custom exceptions in one file
│   └── util/
│       ├── ShortCodeGenerator.java      # Random and collision-safe code generation
│       ├── UrlValidator.java            # URL format and safety validation
│       └── UserAgentParser.java         # Browser/OS/device parsing from User-Agent
├── src/main/resources/
│   ├── application.properties
│   ├── static/
│   │   ├── css/main.css
│   │   └── js/main.js
│   └── templates/
│       ├── index.html                   # Public homepage / shorten form
│       ├── protected-link.html          # Password entry page
│       ├── error/not-found.html
│       └── admin/
│           ├── login.html
│           ├── dashboard.html
│           ├── links.html
│           ├── analytics.html
│           └── api-keys.html
└── pom.xml
```

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8+** running locally or accessible remotely

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-org/urlshortener.git
cd urlshortener
```

### 2. Create the database

```sql
CREATE DATABASE linkvault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate will create the tables automatically on first start (`ddl-auto=update`).

### 3. Configure the application

Edit `src/main/resources/application.properties` (see [Configuration](#configuration) for all options):

```properties
app.base-url=http://localhost:8080
spring.datasource.url=jdbc:mysql://localhost:3306/linkvault?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password_here
app.admin.username=admin
app.admin.password=admin123
app.admin.api-key=lv_sk_change_this_in_production
```

### 4. Build and run

```bash
mvn spring-boot:run
```

The application starts on **port 8080** by default.

| URL | Purpose |
|---|---|
| `http://localhost:8080/` | Public homepage |
| `http://localhost:8080/admin/login` | Admin login |
| `http://localhost:8080/admin/dashboard` | Admin dashboard |

---

## Configuration

All settings live in `application.properties`.

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `app.base-url` | `http://localhost:8080` | Used to build short URLs in responses |
| `app.short-code-length` | `6` | Length of auto-generated short codes |
| `spring.datasource.url` | — | JDBC connection string |
| `spring.datasource.username` | `root` | Database user |
| `spring.datasource.password` | — | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management strategy |
| `app.admin.username` | `admin` | Admin dashboard login username |
| `app.admin.password` | `admin123` | Admin dashboard login password |
| `app.admin.api-key` | `lv_sk_change_this_in_production` | Master API key (has full access) |
| `server.servlet.session.timeout` | `30m` | Admin session duration |

> **Important:** Change all three credentials (`admin.username`, `admin.password`, `admin.api-key`) before deploying to any non-local environment.

---

## API Reference

All API endpoints are under `/api/v1` and require authentication via:

- **Header:** `X-API-Key: <your-api-key>`
- **Query param:** `?apiKey=<your-api-key>`

Invalid or missing keys return `401 Unauthorized`.

---

### Create a short URL

```
POST /api/v1/shorten
Content-Type: application/json
X-API-Key: lv_sk_your_key
```

**Request body:**

```json
{
  "originalUrl": "https://example.com/very/long/path",
  "customCode": "my-link",
  "title": "My Link",
  "analyticsEnabled": true,
  "passwordProtected": false,
  "password": ""
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `originalUrl` | string | Yes | The destination URL |
| `customCode` | string | No | Custom short code (3–30 chars, `[a-zA-Z0-9_-]`) |
| `title` | string | No | Human-readable label |
| `analyticsEnabled` | boolean | No | Default `true` |
| `passwordProtected` | boolean | No | Default `false` |
| `password` | string | Conditional | Required when `passwordProtected` is `true` |

**Response (`201 Created`):**

```json
{
  "success": true,
  "shortUrl": "http://localhost:8080/my-link",
  "shortCode": "my-link",
  "originalUrl": "https://example.com/very/long/path",
  "analyticsEnabled": true,
  "passwordProtected": false,
  "message": "Short URL created successfully"
}
```

---

### List all links

```
GET /api/v1/links?page=0&size=20&sort=createdAt&dir=desc
X-API-Key: lv_sk_your_key
```

Returns a paginated `Page<UrlMapping>`. Page and size are capped at 100.

---

### Get a single link

```
GET /api/v1/links/{id}
X-API-Key: lv_sk_your_key
```

Returns the `UrlMapping` object or `404` if not found.

---

### Delete a link

```
DELETE /api/v1/links/{id}
X-API-Key: lv_sk_your_key
```

Permanently deletes the link and all associated click events.

**Response:**

```json
{ "success": true, "message": "Link deleted successfully" }
```

---

### Toggle a link on/off

```
PATCH /api/v1/links/{id}/toggle
Content-Type: application/json
X-API-Key: lv_sk_your_key

{ "active": false }
```

Disabled links return `410 Gone` when visited.

---

### Get link analytics

```
GET /api/v1/links/{id}/analytics?days=30
X-API-Key: lv_sk_your_key
```

`days` is clamped to `[1, 365]`. Returns an `AnalyticsSummary`:

```json
{
  "urlMappingId": 42,
  "shortCode": "my-link",
  "title": "My Link",
  "totalClicks": 1200,
  "clicksToday": 14,
  "clicksThisWeek": 87,
  "dailyData": [{ "date": "2026-04-24", "count": 14 }],
  "browserBreakdown": { "Chrome": 800, "Safari": 300, "Firefox": 100 },
  "deviceBreakdown": { "DESKTOP": 700, "MOBILE": 450, "TABLET": 50 },
  "osBreakdown": { "Windows 10": 600, "iOS": 300, "Android": 200 },
  "referrerBreakdown": { "twitter.com": 400, "Direct": 600 },
  "recentClicks": [
    {
      "browser": "Chrome",
      "os": "Windows 10",
      "deviceType": "DESKTOP",
      "referrer": "twitter.com",
      "clickedAt": "2026-04-25 10:32:01"
    }
  ]
}
```

---

### Check short code availability

```
GET /api/v1/check/{code}
X-API-Key: lv_sk_your_key
```

```json
{ "code": "my-link", "available": false }
```

---

### Global stats

```
GET /api/v1/stats
X-API-Key: lv_sk_your_key
```

```json
{
  "totalLinks": 350,
  "activeLinks": 312,
  "totalClicks": 48200,
  "clicksToday": 420,
  "protectedLinks": 18,
  "analyticsLinks": 298,
  "topLinks": [],
  "last7DaysTrend": [{ "date": "2026-04-24", "count": 420 }]
}
```

---

## Admin Dashboard

Access at `/admin/login` using the credentials from `application.properties`.

| Route | Description |
|---|---|
| `/admin/dashboard` | Overview stats and 7-day click trend |
| `/admin/links` | Paginated link table with enable/disable/delete actions |
| `/admin/analytics` | Global analytics view |
| `/admin/api-keys` | Create, view, and revoke API keys |
| `/admin/logout` | Log out and invalidate the session |

---

## Security Model

LinkVault uses a layered security approach:

**Public endpoints** — no authentication required:
- `GET /` — homepage
- `POST /shorten` and `POST /api/shorten/web` — URL creation (web form and AJAX)
- `GET /{shortCode}` — redirect
- `GET /protected/{shortCode}` and `POST /protected/{shortCode}` — password entry

**Admin endpoints** (`/admin/**`) — protected by Spring Security form login with an in-memory user. Sessions expire after 30 minutes. CSRF protection is active.

**API endpoints** (`/api/v1/**`) — protected by `ApiKeyAuthFilter`, which runs before the standard auth filter. The filter accepts the key from the `X-API-Key` header or `?apiKey=` query parameter, validates it against the database (or matches the master key), and injects it as a request attribute for downstream use. The API layer is stateless — no session is created.

**Link passwords** are hashed with BCrypt (strength 12) before storage. Raw passwords are never persisted.

**CSRF** is disabled only for `/api/**` routes. All browser-facing form endpoints retain CSRF protection.

---

## Data Model

### `url_mappings`

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `short_code` | VARCHAR(30) UNIQUE | The short identifier |
| `original_url` | VARCHAR(2048) | Destination URL |
| `title` | VARCHAR(255) | Optional label |
| `password_hash` | VARCHAR(255) | BCrypt hash, null if not protected |
| `password_protected` | BOOLEAN | Whether protection is enabled |
| `analytics_enabled` | BOOLEAN | Whether clicks are tracked |
| `click_count` | BIGINT | Running total of redirects |
| `active` | BOOLEAN | Whether the link is enabled |
| `api_key` | VARCHAR(64) | API key that created the link |
| `last_accessed_at` | DATETIME | Timestamp of last click |
| `created_at` | DATETIME | Auto-set on insert |
| `updated_at` | DATETIME | Auto-set on update |

Indexes: `short_code` (unique), `created_at`, `api_key`.

### `click_events`

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `url_mapping_id` | BIGINT FK | Parent link |
| `ip_address` | VARCHAR(64) | Hashed/anonymized IP |
| `user_agent` | VARCHAR(512) | Raw User-Agent string |
| `browser` | VARCHAR(64) | Parsed browser name |
| `operating_system` | VARCHAR(64) | Parsed OS name |
| `device_type` | ENUM | `DESKTOP`, `MOBILE`, `TABLET`, `UNKNOWN` |
| `referrer` | VARCHAR(512) | HTTP Referer header value |
| `country` | VARCHAR(64) | Geo-derived country (if enabled) |
| `clickedAt` | DATETIME | Auto-set on insert |

Indexes: `url_mapping_id`, `clickedAt`, `country`.

---

## Error Handling

`GlobalExceptionHandler` maps all custom exceptions to appropriate HTTP responses:

| Exception | HTTP Status | When |
|---|---|---|
| `ShortCodeNotFoundException` | 404 Not Found | Short code doesn't exist |
| `ShortCodeAlreadyExistsException` | 409 Conflict | Custom code is already taken |
| `InvalidApiKeyException` | 401 Unauthorized | Missing or revoked API key |
| `InvalidPasswordException` | 403 Forbidden | Wrong password for a protected link |
| `LinkDisabledException` | 410 Gone | Link exists but has been deactivated |
| `InvalidUrlException` | 400 Bad Request | Malformed or unsafe URL |
| `RateLimitExceededException` | 429 Too Many Requests | API key rate limit hit |

Browser requests that hit a missing short code are shown the custom `error/not-found.html` template rather than a raw error response.

---

## Production Checklist

- [ ] Set `app.base-url` to your actual domain (e.g., `https://lnk.yourdomain.com`)
- [ ] Change `app.admin.username` and `app.admin.password` to strong credentials
- [ ] Rotate `app.admin.api-key` to a securely generated value (e.g., `openssl rand -hex 32`)
- [ ] Set `spring.datasource.password` from an environment variable rather than hardcoding it
- [ ] Change `spring.jpa.hibernate.ddl-auto` to `validate` once the schema is stable
- [ ] Set `spring.thymeleaf.cache=true` for performance
- [ ] Set `spring.jpa.show-sql=false` (already the default)
- [ ] Place the app behind a reverse proxy (nginx / Caddy) with TLS termination
- [ ] Configure a connection pool (HikariCP settings) appropriate for your traffic
- [ ] Set up log rotation and external log shipping
