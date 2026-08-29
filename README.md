# URL Shortener (AI-Assisted) — Current Version: Iteration 4 Custom Aliases + Expiry

**Date:** 2026-08-29

## Status: Optional custom aliases and expiry support

This project is now in its **fourth iteration**. It keeps the iteration-1 behavior intact while adding two opt-in features for callers who want more control over the short-link contract.

> Current interpretation: links can either use an auto-generated code or a caller-provided alias, and expiry is only applied when the caller explicitly sets `expiresInSeconds`.

This iteration adds predictable alias validation and optional TTL enforcement without changing the old behavior for existing callers.

## What this version does

- `POST /api/shorten` — accepts a long URL and optionally a `customAlias` and `expiresInSeconds`.
- Auto-generated short codes still work exactly as before when the caller omits `customAlias`.
- `GET /{shortCode}` — redirects to the original long URL, increments the click count for valid links, and returns `410 Gone` when the link has expired.
- `GET /api/stats/{shortCode}` — returns analytics data for the short code, including click count and timestamps.
- Duplicate custom aliases are rejected with `409 Conflict` instead of silently creating a second record.
- Alias validation enforces alphanumeric-only values, bounded length, and reserved-name rejection.
- Expiry only applies when a positive `expiresInSeconds` is provided; otherwise the link never expires by default.
- Reuses the existing validation and 404 behavior for invalid or missing codes.

## Iteration 4 decisions and assumptions

- Default TTL behavior: no expiry unless the caller explicitly sets `expiresInSeconds`.
- Alias format: uppercase/lowercase letters and digits only.
- Alias length: 4–32 characters.
- Reserved names rejected: `api`, `health`, `shorten`, `stats`, and any future route collisions should be prohibited.
- Duplicate alias behavior: reject with `409 Conflict` rather than auto-suffixing.
- Expiry trade-off: lazy enforcement on resolve only; no scheduled cleanup job is included in this iteration.
- Future enhancement: expired aliases remain reserved until cleanup, and a nightly cleanup job removes expired records so those aliases can be reused later.

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
  controller/UrlShortenerController.java        - REST endpoints, redirect handling, and stats endpoint
  service/UrlShortenerService.java              - shorten, resolve, alias validation, and expiry logic
  model/UrlMapping.java                         - persisted mapping with click count, last-accessed timestamp, and optional expiry
  repository/UrlMappingRepository.java          - repository with atomic counter updates
  dto/ShortenRequest.java, ShortenResponse.java, StatsResponse.java
  exception/ShortCodeNotFoundException.java, DuplicateAliasException.java, UrlExpiredException.java, GlobalExceptionHandler.java
src/main/resources/db/migration/
  V1__init_schema.sql                           - initial PostgreSQL schema
  V2__add_expiry_to_url_mapping.sql            - adds optional expiry column
src/test/java/com/schwab/urlshortener/UrlShortenerServiceTest.java
src/test/resources/application.properties      - H2 test configuration
ITERATIONS.md                                   - full iteration log and decision history
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

**Shorten with a custom alias and expiry**

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/some/very/long/path",
    "customAlias": "demo42",
    "expiresInSeconds": 86400
  }'
```

Response:

```json
{
  "shortCode": "demo42",
  "shortUrl": "/demo42",
  "longUrl": "https://www.example.com/some/very/long/path"
}
```

**Follow the short link**

```bash
curl -i http://localhost:8080/demo42
# HTTP/1.1 302 Found
# Location: https://www.example.com/some/very/long/path
```

**Read link statistics**

```bash
curl http://localhost:8080/api/stats/demo42
```

Example response:

```json
{
  "shortCode": "demo42",
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

See [`ITERATIONS.md`](ITERATIONS.md) for the full project record, including the initial baseline, PostgreSQL persistence work, and the later iteration-4 alias and expiry decisions.
