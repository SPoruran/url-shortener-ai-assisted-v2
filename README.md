# URL Shortener (AI-Assisted) — Iteration 1: Basic Functionality

## Status: Simplified Foundation

This is the **first iteration** of the project, starting with a simplified version
focused on the core requirement:

> Accepts a long URL and returns a shortened, unique, redirectable code.

This foundation provides a working baseline. Additional features — persistence,
custom aliases, expiry, analytics, rate limiting, auth, containerization — will
be added in subsequent iterations as the project naturally evolves. See [`ITERATIONS.md`](ITERATIONS.md)
for the running log of what changed in each pass and why.

## What this version does

- `POST /api/shorten` — accepts a long URL, returns a unique short code and
  short URL.
- `GET /{shortCode}` — redirects (HTTP 302) to the original long URL.
- Validates that the submitted URL is a well-formed `http(s)` URL.
- Generates a unique 7-character Base62 code, with collision detection
  against existing codes.
- Returns a clean `404` with a JSON error body when a short code doesn't
  exist.

## What comes in future iterations

- Database persistence — currently uses an in-memory `ConcurrentHashMap`.
- Custom/vanity short codes.
- Expiry and TTL on links.
- Click/analytics tracking.
- Authentication and per-user link ownership.
- Docker Compose setup.
- Pagination, listing, and delete endpoints.

Each feature will be added as the project evolves through documented iterations.

## Tech stack

- Java 17
- Spring Boot 3.3 (Web, Validation)
- Maven
- JUnit 5 (basic service-layer tests)

## Project structure

```
src/main/java/com/schwab/urlshortener/
  UrlShortenerApplication.java     - Spring Boot entry point
  controller/UrlShortenerController.java   - REST endpoints
  service/UrlShortenerService.java         - code generation + in-memory store
  model/UrlMapping.java                    - internal mapping record
  dto/ShortenRequest.java, ShortenResponse.java
  exception/ShortCodeNotFoundException.java, GlobalExceptionHandler.java
src/test/java/com/schwab/urlshortener/UrlShortenerServiceTest.java
```

## Running locally

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

## Next steps

See [`ITERATIONS.md`](ITERATIONS.md) — each future change (persistence,
Docker Compose, custom codes, analytics, etc.) gets its own dated entry
there, plus a corresponding commit, so the history documents the
AI-assisted engineering process end to end.
