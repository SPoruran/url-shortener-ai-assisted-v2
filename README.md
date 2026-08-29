# URL Shortener (AI-Assisted) — Iteration 2: PostgreSQL Persistence + Duplicate URL Detection

## Status: Persistent Brownfield Upgrade

This is the **second iteration** of the project. It keeps the same API and behavior from iteration 1, but replaces the in-memory `ConcurrentHashMap` with a PostgreSQL-backed persistence layer and adds duplicate long-URL detection.

> Core requirement: accept a long URL, return a shortened redirectable code, and resolve that code back to the original URL.

This iteration is intentionally a brownfield upgrade: the externally visible endpoints remain stable while the storage mechanism changes underneath.

## What this version does

- `POST /api/shorten` — accepts a long URL, persists it, and returns a short code and short URL.
- `GET /{shortCode}` — redirects (HTTP 302) to the original long URL.
- Validates submitted URLs using `http(s)` rules.
- Stores mappings in PostgreSQL via JPA.
- Detects duplicate long URLs and returns the same short code instead of creating a second record.
- Generates a unique 7-character Base62 code when a new link is created.
- Returns structured 400/404 error responses for invalid input and missing short codes.

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
  controller/UrlShortenerController.java        - REST endpoints
  service/UrlShortenerService.java              - persistence-aware shorten/resolve logic
  model/UrlMapping.java                         - JPA-backed persisted mapping entity
  repository/UrlMappingRepository.java          - repository for short-code + long-url lookups
  dto/ShortenRequest.java, ShortenResponse.java
  exception/ShortCodeNotFoundException.java, GlobalExceptionHandler.java
src/test/java/com/schwab/urlshortener/UrlShortenerServiceTest.java
src/test/resources/application.properties      - H2 test configuration
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

**Shorten the same URL again**

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/some/very/long/path"}'
```

This returns the same short code instead of creating a duplicate entry.

**Follow the short link**

```bash
curl -i http://localhost:8080/aZ3kQ9x
# HTTP/1.1 302 Found
# Location: https://www.example.com/some/very/long/path
```

## Running tests

```bash
mvn test
```

## What comes next

See [`ITERATIONS.md`](ITERATIONS.md) for the documented progression. The next iteration will continue the roadmap with additional concerns such as custom aliases, expiry, click tracking, rate limiting, and deployment improvements.
