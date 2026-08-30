# Testing Strategy - URL Shortener Service

## 1. Testing Objectives

The project uses a layered test strategy to validate the application from the business-logic boundary outward:

- Verify core short-link behavior remains correct across refactors
- Validate domain rules such as duplicate detection, alias rules, and expiry handling
- Confirm API behavior through controller-level validation and runtime checks
- Ensure the application still builds cleanly in Docker and local environments

This aligns with the assignment requirement to show disciplined, evidence-based validation rather than unchecked AI-generated code.

---

## 2. Test Scope

### 2.1 Unit Tests
Focus on service-level logic without network or external dependencies.

Covered behaviors:
- URL shortening generates valid codes
- Duplicate long URLs return the existing short code
- Custom aliases are validated and normalized
- Duplicate aliases throw domain exceptions
- Missing short codes throw `ShortCodeNotFoundException`
- Expired URLs are rejected
- Click count increments correctly

### 2.2 Integration / Functional Tests
Exercise real repository and persistence logic using the H2 test profile.

Covered behaviors:
- Save and retrieve `UrlMapping` entities
- Verify repository queries for `findByLongUrl` and `findByShortCode`
- Confirm transactional updates for click tracking
- Validate end-to-end service workflows

### 2.3 API Validation Tests
Validate HTTP behavior through the controller layer and request/response contract.

Covered behaviors:
- `POST /api/shorten` creates a new short URL
- `GET /{shortCode}` redirects correctly
- `GET /api/stats/{shortCode}` returns structured analytics
- Validation errors are returned consistently

### 2.4 Build Verification
The project must compile and package successfully using Maven and Docker.

This includes:
- `mvn test`
- `mvn package`
- `docker compose up --build`

---

## 3. Test Cases

### 3.1 ShortCodeGenerator Test Coverage
The generator utility is validated for:
- length = 7
- character set restrictions
- invalid values such as null, blank, wrong length, and invalid characters

Sample assertions:
- `generate()` returns exactly 7 chars
- `isValid(null)` returns false
- `isValid("abc/def")` returns false
- `isValid("ABC1234")` returns true when within allowed format

### 3.2 UrlShorteningService Test Coverage
Key tests include:
- creates new short code for new URL
- reuses existing short code for duplicate long URL
- rejects duplicate custom alias
- allows valid custom alias values
- resolves `expiresInSeconds` correctly

### 3.3 UrlRedirectService Test Coverage
Key tests include:
- resolves existing short code to long URL
- increments click count
- throws `ShortCodeNotFoundException` when code does not exist
- throws `UrlExpiredException` when URL expired

### 3.4 UrlAnalyticsService Test Coverage
Key tests include:
- returns full stats record for valid short code
- throws not-found exception for invalid code

---

## 4. Testing Tools and Setup

### 4.1 Java / Maven
- JUnit 5 for tests
- Spring Boot test support for application context validation
- H2 in-memory database for isolated test execution

### 4.2 Running Tests
Use:

```bash
mvn test
```

Or to compile without running tests:

```bash
mvn -DskipTests package
```

### 4.3 Docker Validation
The project supports runtime validation through Docker Compose:

```bash
docker compose up --build
```

This validates:
- application startup
- database connectivity
- Flyway migrations
- container health for the stack

---

## 5. Quality Gates

The following gates must be satisfied before considering the work ready:

1. Project compiles successfully
2. All unit tests pass
3. No duplicate class or package import issues remain
4. Runtime build works in Docker Compose
5. No stale test imports or historical package references remain
6. Domain rules remain unchanged after refactoring

---

## 6. Common Failure Patterns and Fixes

### Import drift
Issue:
- Tests or code still reference the old `service.ShortCodeGenerator` package after it was moved to `util`.

Fix:
- Update imports to `com.schwab.urlshortener.util.ShortCodeGenerator`

### Duplicate class files
Issue:
- A stale duplicate class remains under the wrong package and causes compilation failures.

Fix:
- Remove historical duplicate classes and keep a single source of truth.

### Docker runtime errors
Issue:
- Database binding conflicts or container startup failures due to port collisions or stale containers.

Fix:
- Stop local PostgreSQL services or clean old containers before restarting Docker Compose.

---

## 7. Testing Philosophy

The testing approach follows the assignment’s AI-assisted engineering expectations:

- validate behavior over mock-heavy testing
- prefer real domain logic where possible
- keep tests focused on contract and business outcomes
- treat refactoring as a behavior-preserving activity
- fix root causes instead of masking compiler errors

This ensures the application is not only functional, but also maintainable and safe to evolve.

---

## 8. Final Validation Summary

The application is considered validated when:

- all service logic tests pass
- redirect and analytics flows behave correctly
- code compiles without stale imports or duplicates
- Docker build runs successfully
- the project reflects the intended architecture and SOLID refactor goals

