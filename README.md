# URL Shortener (AI-Assisted) — Current Version: Iteration 5 Docker Compose + PostgreSQL

**Date:** 2026-08-29

## Status: Production-style app + PostgreSQL runtime with Docker Compose

This project is now in its **fifth iteration**. The app keeps the earlier URL-shortening behavior intact while moving the runtime to a proper multi-service Docker Compose setup using PostgreSQL for the main application and H2 only for tests.

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

## Iteration 5 decisions and assumptions

- Runtime database: PostgreSQL in Docker Compose.
- Test database: H2 only under the Maven test profile.
- Compose service-to-service access uses the internal `db` hostname, not `localhost`.
- Host port 5432 is reserved for the Docker Postgres container; local Postgres should be stopped to avoid conflicts.
- Expiry behavior remains the same as iteration 4: no expiry unless the caller sets a positive TTL and expired links return `410 Gone` when accessed.

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

### Docker Compose (current runtime setup)

This project now runs as two services:
- `db` — PostgreSQL
- `app` — the Spring Boot application

From the project root:

```bash
docker compose up --build
```

This starts the application on `http://localhost:8080` and the database on `localhost:5432`.

The app connects to PostgreSQL using the Compose service name `db`, not `localhost`, because inside the container network `localhost` refers to the app container itself.

### Local Java run (without Compose)

If you want to run the app directly on your machine instead of via Docker Compose:

```bash
mvn spring-boot:run
```

### If the Docker database needs to be reset

Use the Compose stack reset instead of a direct local Flyway command:

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

This rebuilds the Postgres container and the app from the current project state without leaving stale local database state behind.

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

The main runtime configuration uses PostgreSQL. H2 remains test-only and is activated via the test profile.

```bash
mvn test
```

This keeps H2 out of the main `application.properties` and out of the Compose runtime stack.

## Project history

See [`ITERATIONS.md`](ITERATIONS.md) for the full project record, including the baseline, PostgreSQL persistence, custom alias and expiry work, and the current Docker Compose orchestration update.
