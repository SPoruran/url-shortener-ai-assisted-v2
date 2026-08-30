# Engineering Summary - URL Shortener Service

## 1. Project Outcome

This project evolved from a basic in-memory URL shortener into a production-oriented Spring Boot application using PostgreSQL, Flyway, layered services, and domain-oriented validation. The final implementation focuses on robust behavior, clean separation of responsibilities, and sustainable refactoring practices.

The work follows the intent of the assignment: build a maintainable Java application that demonstrates engineering discipline, scenario-based iteration, and evidence-based delivery.

---

## 2. Scope Delivered

### Core functionality
- Create short URLs from long URLs
- Support optional custom aliases
- Reuse existing short codes for duplicate long URLs
- Redirect using short codes
- Track click count and last access time
- Support optional expiry for URLs
- Expose analytics endpoints

### Architecture and refactoring
- Split responsibilities across dedicated services
- Introduced a facade for high-level orchestration
- Extracted helpers for validation and expiry logic
- Stabilized dependency flow using interfaces
- Kept controllers thin and domain-focused

### Operational readiness
- PostgreSQL runtime configuration
- Flyway schema migration support
- Docker Compose setup for local execution
- H2-based testing configuration for isolated validation

---

## 3. Design Choices

### 3.1 Layered design
The application follows a clear layered structure:

- Controllers handle HTTP concerns
- Facade provides a stable business API surface
- Services encapsulate each domain responsibility
- Repository handles persistence and data access
- Utilities hold reusable helper logic

This keeps each layer focused and reduces accidental coupling.

### 3.2 Single Responsibility Principle
The service layer was refactored so that each service has a clear purpose:

- `UrlShorteningService` handles creation and shortening logic
- `UrlRedirectService` handles resolution and click tracking
- `UrlAnalyticsService` handles read-only stats retrieval

This prevents one service from becoming a “god service” with mixed responsibilities.

### 3.3 Dependency Inversion Principle
Service interfaces were introduced so that higher-level components depend on abstractions rather than implementations.

This helps with:
- testability
- future implementation swapping
- lower coupling
- safer refactoring

### 3.4 Utility extraction
Reusable logic was moved out of the service layer into focused helper classes:

- `ShortCodeGenerator` for code generation/validation
- `AliasValidator` for alias rules
- `LinkExpiryPolicy` for expiry evaluation

This improves clarity and reduces duplicate logic.

---

## 4. Key Technical Decisions

### 4.1 Duplicate URL handling
If the same long URL is shortened again, the service returns the existing short code instead of creating a new mapping. This preserves consistency and reduces storage duplication.

### 4.2 Alias validation
Custom aliases are validated for:
- allowed characters
- minimum/maximum length
- reserved names and collisions
- format correctness

### 4.3 Expiry behavior
Short links can include a TTL. Expired links remain reserved but are rejected when accessed. The project intentionally avoids background cleanup to keep behavior deterministic and simple.

### 4.4 Transactional consistency
State-changing operations, especially redirect resolution, are wrapped in `@Transactional` to ensure click count and timestamp updates remain consistent.

---

## 5. Implementation Quality

### 5.1 Correctness
The project includes validations for:
- invalid URL input
- duplicate alias attempts
- missing short codes
- expired short links
- invalid generated codes

### 5.2 Maintainability
The codebase was trimmed of stale duplicates, unneeded historical artifacts, and inconsistent package usage. This reduces confusion and helps future changes remain safe.

### 5.3 Testability
The architecture is designed to support service-level testing and application-layer validation with minimal friction. The project keeps behavior verification grounded in real logic instead of mock-only outcomes.

---

## 6. Problems Addressed During Delivery

Several issues were handled during implementation and cleanup, including:

- duplicate `ShortCodeGenerator` classes
- stale package imports after refactoring
- Docker runtime conflicts from local PostgreSQL instances
- migration and startup issues during Compose validation
- refactor artifacts left behind from earlier iterations

The project was corrected through targeted root-cause fixes, not by masking errors or ignoring failing build outputs.

---

## 7. Validation Summary

The application was validated through:

- Maven compilation checks
- JUnit-based service testing
- runtime build verification through Docker Compose
- import cleanup and class deduplication
- dependency and refactor safety checks

This approach follows the assignment’s expectation that engineering work should be evidence-based and demonstrably correct.

---

## 8. Final Assessment

This project is now in a strong engineering state:

- behaviorally complete for the URL shortener domain
- cleanly organized by responsibility
- easier to evolve and extend
- validated through repeatable build and test flow
- aligned with enterprise-style refactoring practices

It demonstrates not only the ability to implement features, but also the discipline to maintain a healthy codebase over time.

---

## 9. Recommended Next Evolution

Possible future enhancements:
- scheduled cleanup for expired aliases
- rate limiting and abuse protection
- analytics dashboards or reporting
- QR code generation
- API key or auth layer for public deployments

These remain optional and do not block the current implementation.
