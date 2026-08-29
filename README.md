# URL Shortener (AI-Assisted) — Current Version: Iteration 3 Analytics

## Status: Minimal analytics for link usage

This project is now in its **third iteration**, where the requirement was intentionally clarified before implementation. The stakeholder request was vague — “Add analytics to the URL shortener” — so the project scope was narrowed to a defensible, low-risk interpretation.

> Current interpretation: analytics means tracking link usage via a click count and last-accessed timestamp for each short code.

This iteration keeps the existing URL shortener behavior intact while adding basic operational telemetry for successful resolves.

## What this version does

- `POST /api/shorten` — accepts a long URL and returns a short code.
- `GET /{shortCode}` — redirects to the original long URL and increments the click count.
- `GET /api/stats/{shortCode}` — returns the short code, original long URL, click count, created timestamp, and last-accessed timestamp.
- Reuses the existing validation and 404 behavior for invalid or missing codes.
- Stores the analytics data in the same persisted model used in iteration 2.
- Uses atomic increment semantics to avoid lost hits under concurrency.

## Scope decision

The project deliberately excludes broader analytics features such as:

- unique visitor counting
- referrer tracking
- device or browser breakdowns
- geographic analysis
- dashboards or charts
- external analytics providers
- per-user or per-session attribution

These were excluded because they would move the project beyond a small, auditable v1 analytics feature into a much larger product-analytics problem.

## Tech stack

- Java 17
- Spring Boot 3.3
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL
- Maven
- JUnit 5

## Project structure

```
src/main/java/com/schwab/urlshortener/
  UrlShortenerApplication.java                  - Spring Boot entry point
  controller/UrlShortenerController.java        - REST endpoints and stats endpoint
  service/UrlShortenerService.java              - shorten, resolve, and click tracking logic
  model/UrlMapping.java                         - persisted mapping with click count and last-accessed timestamp
  repository/UrlMappingRepository.java          - repository with atomic counter updates
  dto/ShortenRequest.java, ShortenResponse.java, StatsResponse.java
  exception/ShortCodeNotFoundException.java, GlobalExceptionHandler.java
src/test/java/com/schwab/urlshortener/UrlShortenerServiceTest.java
src/test/resources/application.properties      - H2 test configuration
ANALYTICS_DECISION.md                           - ambiguity framing and scope rationale
ITERATIONS.md                                   - historical iteration log
```

## Running locally

### 1) Start PostgreSQL with Docker

```bash
docker run --name urlshortener-postgres \
  -e POSTGRES_DB=urlshortener \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16
```

### 2) Start the app

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

### If the local PostgreSQL schema is stale or the migration fails

Use Flyway to rebuild the database from the repository migration scripts. For Flyway v10+, `clean` is disabled by default, so you must explicitly allow it for this reset step:

```bash
mvn flyway:clean flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/urlshortener \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres \
  -Dflyway.cleanDisabled=false
```

This is the correct recovery step when a brownfield schema change left an old `url_mapping` table in a bad state, especially after a failed `click_count` migration.

## API examples

**Shorten a URL**

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/some/very/long/path"}'
```

Response:

```json
{
  "shortCode": "aZ3kQ9x",
  "shortUrl": "/aZ3kQ9x",
  "longUrl": "https://www.example.com/some/very/long/path"
}
```

**Follow the short link**

```bash
curl -i http://localhost:8080/aZ3kQ9x
# HTTP/1.1 302 Found
# Location: https://www.example.com/some/very/long/path
```

**Read link statistics**

```bash
curl http://localhost:8080/api/stats/aZ3kQ9x
```

Example response:

```json
{
  "shortCode": "aZ3kQ9x",
  "longUrl": "https://www.example.com/some/very/long/path",
  "clickCount": 1,
  "createdAt": "2026-08-29T21:00:00Z",
  "lastAccessedAt": "2026-08-29T21:05:00Z"
}
```

## Running tests

```bash
mvn test
```

## Project history

See [`ITERATIONS.md`](ITERATIONS.md) for the full historical record of the baseline, persistence migration, analytics scoping decisions, and the brownfield migration fix for the `click_count` schema issue.

### Bug found and fixed

A schema migration bug was encountered during the analytics iteration: adding a new `click_count` column with `NOT NULL` to an existing PostgreSQL table failed because existing rows had `NULL` values.

The fix was to stop using Hibernate auto-update for schema changes and instead manage the schema explicitly with Flyway migrations, creating the table with a safe default value (`0`) for existing records.
