# URL Shortener

A Spring Boot URL shortener with PostgreSQL persistence, Flyway database migrations, optional custom aliases and expiry, and basic redirect analytics.

## Features

- Create a short URL from a long HTTP or HTTPS URL.
- Reuse the existing short code for a duplicate long URL.
- Support validated custom aliases and optional expiry periods.
- Redirect `GET /{shortCode}` requests to the stored URL.
- Return `410 Gone` for expired links and `404 Not Found` for missing links.
- Track successful redirects with a click count and last-access timestamp.

## Technology

- Java 17 and Spring Boot 3.3
- Spring Web, Validation, and Data JPA
- PostgreSQL, Flyway, and H2 for tests
- Docker Compose, Maven, and JUnit 5

## Project Structure

```
src/main/java/com/schwab/urlshortener/
  controller/                                    - create, redirect, and analytics endpoints
  service/                                       - shortening, redirect, analytics, validation, and expiry services
  repository/UrlMappingRepository.java           - persistence and atomic click-count update
  model/UrlMapping.java                          - persisted URL mapping
  dto/                                           - request and response objects
  exception/                                     - domain exceptions and HTTP error handling
  util/ShortCodeGenerator.java                   - short-code generation
src/main/resources/db/migration/
  V1__init_schema.sql                            - initial PostgreSQL schema
  V2__add_expiry_to_url_mapping.sql              - expiry support
src/test/java/com/schwab/urlshortener/           - unit tests
docs/                                            - setup, architecture, testing, and decision records
```

## Run Locally

### Docker Compose

This project runs as two services: the Spring Boot application and PostgreSQL.

On Windows, start Docker Desktop and wait until it is running before continuing. Verify the Docker daemon is available:

```cmd
docker version
```

The output must include both `Client` and `Server` sections. If it reports an error for `dockerDesktopLinuxEngine`, restart Docker Desktop and ensure its WSL 2 based Linux engine is enabled.

From the project root:

```cmd
docker compose up --build
```

This starts the application on `http://localhost:8080` and the database on `localhost:5432`.

The app connects to PostgreSQL using the Compose service name `db`, not `localhost`, because inside the container network `localhost` refers to the app container itself.

### Local Java Run

If you want to run the app directly on your machine instead of via Docker Compose:

```cmd
mvn spring-boot:run
```

The external PostgreSQL setup has not been validated end-to-end from this local environment. See the [setup guide](docs/SETUP.md) for configuration details and constraints.

### If the Docker database needs to be reset

Use the Compose stack reset instead of a direct local Flyway command:

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

This rebuilds the Postgres container and the app from the current project state without leaving stale local database state behind.

## API Examples

`longUrl` is required. Both `customAlias` and `expiresInSeconds` are optional: omit them to generate a code without expiry, or provide either value when needed.

**Shorten a URL without optional fields**

```cmd
curl.exe -X POST "http://localhost:8080/api/shorten" -H "Content-Type: application/json" -d "{\"longUrl\":\"https://www.example.com/some/very/long/path\"}"
```

Response (`201 Created`):

```json
{"shortCode":"KtGvWS3","shortUrl":"/KtGvWS3","longUrl":"https://www.example.com/some/very/long/path"}
```

**Shorten with optional custom alias and expiry**

```cmd
curl.exe -X POST "http://localhost:8080/api/shorten" -H "Content-Type: application/json" -d "{\"longUrl\":\"https://www.example.com/new-unique-page-123\",\"customAlias\":\"demo42\",\"expiresInSeconds\":86400}"
```

Response:

```json
{
  "shortCode": "demo42",
  "shortUrl": "/demo42",
  "longUrl": "https://www.example.com/new-unique-page-123"
}
```

**Follow the short link**

```cmd
curl.exe -i -X GET "http://localhost:8080/demo42"
# HTTP/1.1 302 Found
# Location: https://www.example.com/new-unique-page-123
```

Or open `http://localhost:8080/demo42` in a browser. The browser should redirect to `https://www.example.com/new-unique-page-123`.

**Read link statistics**

```cmd
curl.exe -X GET "http://localhost:8080/api/stats/demo42"
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

## Test

The main runtime configuration uses PostgreSQL. H2 remains test-only and is activated via the test profile.

```cmd
mvn test
```

To run one test class:

```cmd
mvn test -Dtest=ShortUrlServiceTest
```

Maven must be installed and available on `PATH`. Test reports are written to `target/surefire-reports/`.

## Documentation

- [Setup guide](docs/SETUP.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Testing](docs/TESTING.md)
- [Analytics scope decision](docs/ANALYTICS_DECISION.md)
- [Engineering summary](docs/ENGINEERING_SUMMARY.md)
- [Project scenarios](docs/SCENARIOS.md)
- [AI usage tracker](docs/AI_USAGE_TRACKER.md)
