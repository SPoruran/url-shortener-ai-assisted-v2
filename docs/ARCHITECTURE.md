# Architecture Overview - URL Shortener Service

## 1. System Components

### 1.1 Presentation Layer (Controllers)
- **UrlCreateController** - Handles POST `/api/shorten` requests; orchestrates URL shortening with optional custom aliases and expiry
- **UrlRedirectController** - Handles GET `/{shortCode}` requests; resolves short codes to long URLs with redirect response
- **AnalyticsController** - Handles GET `/api/stats/{shortCode}` requests; retrieves click statistics and metadata

### 1.2 Facade Layer
- **UrlShortenerFacade** - Centralized public API that aggregates shortening, redirect, and analytics operations; transforms service-layer results into unified DTOs; implements dependency inversion by depending on service interfaces rather than concrete implementations

### 1.3 Business Logic Layer (Services)
**Service Interfaces:**
- `ShorteningService` - Contract for URL shortening operations
- `RedirectService` - Contract for redirect resolution
- `AnalyticsService` - Contract for statistics retrieval

**Service Implementations:**
- **UrlShorteningService** - Implements `ShorteningService`
  - Creates short codes (auto-generated or custom)
  - Detects and reuses duplicate long URLs
  - Validates custom aliases against reserved names
  - Applies expiry policy
  - Enforces `@Transactional` for state changes

- **UrlRedirectService** - Implements `RedirectService`
  - Resolves short codes to long URLs
  - Increments click count and last-accessed timestamp
  - Validates URL expiry and throws `UrlExpiredException` if expired
  - Transactional to ensure consistent state

- **UrlAnalyticsService** - Implements `AnalyticsService`
  - Retrieves read-only statistics for a short code
  - Includes click count, creation time, last access time

### 1.4 Helper/Policy Classes
- **AliasValidator** - Validates custom aliases; enforces reserved name rules and character restrictions
- **LinkExpiryPolicy** - Evaluates expiry timestamps; resolves expiry duration to absolute time
- **ShortCodeGenerator** (Util) - Static utility for generating 7-character base62 codes; validates code format

### 1.5 Data Access Layer (Repository)
- **UrlMappingRepository** - JPA repository for CRUD operations on `UrlMapping` entities
  - Custom methods: `findByLongUrl()`, `findByShortCode()`, `incrementClickCount()`

### 1.6 Data Model
- **UrlMapping** (Entity)
  - `id` - Primary key
  - `shortCode` - Unique 7-character code (indexed)
  - `longUrl` - Original URL (indexed for deduplication)
  - `clickCount` - Number of redirects
  - `createdAt` - Timestamp of creation
  - `lastAccessedAt` - Timestamp of most recent access
  - `expiresAt` - Optional expiry timestamp (null = no expiry)

### 1.7 DTOs (Data Transfer Objects)
- **ShortenRequest** - Input: `longUrl` (required), `customAlias` (optional), `expiresInSeconds` (optional)
- **ShortenResponse** - Output: `shortCode`, `shortUrl`, `longUrl`
- **StatsResponse** - Output: `shortCode`, `longUrl`, `clickCount`, `createdAt`, `lastAccessedAt`

### 1.8 Exception Handling
- **ShortCodeNotFoundException** - Thrown when a short code does not exist
- **UrlExpiredException** - Thrown when a URL has expired
- **DuplicateAliasException** - Thrown when a custom alias already exists
- **GlobalExceptionHandler** - Centralized exception handler for consistent error responses

---

## 2. Technology Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.3.x |
| Language | Java 17 |
| Persistence | Spring Data JPA, PostgreSQL (runtime), H2 (test) |
| Migrations | Flyway |
| Build | Maven |
| Testing | JUnit 5, Mockito (optional) |
| Runtime | Docker Compose |

---

## 3. Design Patterns & SOLID Principles

### Single Responsibility (S)
- Each service handles one domain concern (shortening, redirects, analytics)
- Controllers delegate to facade; facade delegates to services
- Validators and policies extracted from core services

### Open/Closed (O)
- Facade provides stable public API; services can be extended without changing controllers
- New service implementations can be swapped by updating Spring bean wiring

### Liskov Substitution (L)
- Not directly applicable here (services have no substitutable implementations yet)
- Future: alternative storage backends could implement `UrlMappingRepository`

### Interface Segregation (I)
- Service interfaces are minimal: `ShorteningService`, `RedirectService`, `AnalyticsService`
- No fat interfaces; each service exposes only its relevant operations

### Dependency Inversion (D)
- Controllers and facade depend on service **interfaces**, not concrete classes
- Spring Constructor Injection ensures loose coupling
- High-level modules (facade) depend on abstractions (interfaces)

---

## 4. Key Architectural Decisions

### 4.1 Facade Pattern
**Rationale:** Centralize public domain API and decouple controllers from internal service split
- Controllers depend on a single facade, not three separate services
- Facade transforms service results into unified DTO records
- Simplifies testing and allows service refactoring without controller changes

### 4.2 URL Deduplication
**Rationale:** Efficiency and consistency
- Long URLs are indexed and checked on create
- If a duplicate long URL exists, return the existing short code instead of creating a new entry
- Reduces storage and ensures URL reuse across requests

### 4.3 Custom Alias Support
**Rationale:** User convenience and SEO benefits
- Optional `customAlias` parameter allows memorable short codes (e.g., `/demo42`)
- Validated for length (4-32 chars), alphanumeric only, and no reserved names
- Throws `DuplicateAliasException` if alias already in use

### 4.4 Expiry Policy
**Rationale:** TTL management without background jobs
- Optional `expiresInSeconds` parameter sets absolute expiry time on create
- On redirect, expiry is evaluated; throws `UrlExpiredException` if past expiry
- No scheduled cleanup; expired URLs remain in DB but are inaccessible (future: async cleanup)

### 4.5 Transactional Boundaries
**Rationale:** Consistency and atomicity
- `@Transactional` applied to state-changing operations in service layer
- Redirect resolution increments click count atomically with timestamp update
- No nested transactions; straightforward read/write boundaries

### 4.6 Repository Abstraction
**Rationale:** Test isolation and data access consistency
- Custom repository methods (`findByLongUrl`, `incrementClickCount`) encapsulate query logic
- Facilitates H2 in-memory testing without modifying service logic

---

## 5. Control Flow

### 5.1 Shorten Flow
```
POST /api/shorten (ShortenRequest)
  ↓
UrlCreateController.shorten()
  ↓
UrlShortenerFacade.shorten()
  ↓
UrlShorteningService.shorten()
  ├─ Check if long URL already exists → return existing short code
  ├─ Resolve short code (auto-generate or validate custom alias)
  ├─ Apply expiry policy (calculate expiresAt)
  ├─ Create UrlMapping entity
  └─ Save to repository
  ↓
Transform result to facade DTO
  ↓
Return ShortenResponse (201 Created)
```

### 5.2 Redirect Flow
```
GET /{shortCode}
  ↓
UrlRedirectController.redirect()
  ↓
UrlShortenerFacade.resolve()
  ↓
@Transactional UrlRedirectService.resolve()
  ├─ Look up UrlMapping by shortCode
  ├─ Check expiry policy (throw if expired)
  ├─ Increment click count + update lastAccessedAt
  └─ Return longUrl
  ↓
Create redirect response (302 Found)
  ↓
Return HTTP redirect to longUrl
```

### 5.3 Analytics Flow
```
GET /api/stats/{shortCode}
  ↓
AnalyticsController.stats()
  ↓
UrlShortenerFacade.getStats()
  ↓
UrlAnalyticsService.getStats()
  ├─ Look up UrlMapping by shortCode
  └─ Return UrlStats record
  ↓
Transform to facade DTO
  ↓
Return StatsResponse (200 OK)
```

---

## 6. Database Schema

### Table: `url_mapping`
| Column | Type | Nullable | Constraints |
|--------|------|----------|-------------|
| `id` | BIGSERIAL | NO | PK |
| `short_code` | VARCHAR(7) | NO | UNIQUE, INDEX |
| `long_url` | TEXT | NO | INDEX |
| `click_count` | BIGINT | NO | DEFAULT 0 |
| `created_at` | TIMESTAMP | NO | DEFAULT NOW() |
| `last_accessed_at` | TIMESTAMP | YES | |
| `expires_at` | TIMESTAMP | YES | |

### Indexes
- `idx_short_code` on `short_code` for fast redirect lookup
- `idx_long_url` on `long_url` for deduplication detection

---

## 7. Configuration

### Runtime (PostgreSQL)
**File:** `src/main/resources/application.properties`
- Datasource: PostgreSQL (host, port, database, credentials)
- JPA: `ddl-auto=none` (Flyway manages schema)
- Flyway: auto-migration on startup

### Test (H2 In-Memory)
**File:** `src/test/resources/application-test.properties`
- Datasource: H2 in-memory, Postgres-compatible mode
- JPA: `ddl-auto=create-drop` (reset schema per test)
- Flyway: disabled (JPA handles schema)

---

## 8. Deployment

### Docker Compose
**File:** `docker-compose.yml`
- **Service: `app`** - Spring Boot JAR, port 8080, depends on `db`
- **Service: `db`** - PostgreSQL 15, port 5432, volume persistence

### Build Process
1. Maven compiles code + runs unit tests
2. Docker builds app image with compiled JAR
3. Docker Compose orchestrates app + database startup
4. Flyway auto-migrates schema on container startup

---

## 9. Security & Reliability Considerations

### 9.1 Input Validation
- All DTOs use Jakarta Bean Validation annotations (NotBlank, Pattern, Positive, Size)
- Long URL validated as HTTP/HTTPS with regex
- Custom alias validated for alphanumeric characters and length

### 9.2 Exception Handling
- `GlobalExceptionHandler` catches all domain exceptions
- Returns structured error JSON with appropriate HTTP status codes
- Prevents stack traces leaking to client

### 9.3 Concurrency
- `@Transactional` ensures atomic state changes
- Database constraints (UNIQUE on short_code) prevent race conditions
- Click count increments use atomic SQL UPDATE

### 9.4 Data Persistence
- PostgreSQL provides ACID guarantees
- Flyway migrations versioned; rollback supported (not auto-applied)
- Foreign key constraints could be added for future audit tables

---

## 10. Future Enhancements

### 10.1 Async Cleanup
- Background job to mark/delete expired URLs nightly
- Currently: expired URLs remain in DB (reserved)

### 10.2 Rate Limiting
- API rate limiting per IP/API key
- Prevent short code bruteforce attacks

### 10.3 Custom Domain
- Allow users to specify custom domain (e.g., `short.example.com/{code}`)

### 10.4 Audit Logging
- Log all shorten/redirect operations for compliance
- Separate audit table with service account ownership

### 10.5 Caching
- Redis cache for frequently accessed short codes
- Reduce database load for popular links

### 10.6 QR Code Generation
- Auto-generate QR code for short URL on creation

---

## 11. Monitoring & Observability

### Metrics to Track
- Requests per second (create, redirect, stats)
- Cache hit/miss ratio (if Redis added)
- Database query latency
- Expired URL access attempts

### Logs
- Shorten operations: input URL, generated short code, alias (if custom)
- Redirect operations: short code, target URL, expiry status
- Errors: full stack traces for non-domain exceptions

### Health Checks
- Liveness: application startup complete
- Readiness: database connectivity verified

---

## Summary

The URL Shortener is a layered, modular Spring Boot service that demonstrates SOLID principles, clean architecture, and production-grade engineering practices. The facade pattern provides a stable public API, while service interfaces enable testability and future extensibility. Database-level constraints and transactional boundaries ensure consistency, and comprehensive validation prevents invalid state.
