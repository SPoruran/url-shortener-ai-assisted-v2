# Scenario Decomposition & Execution - URL Shortener

This document demonstrates three scenario types required by the assignment: **greenfield**, **brownfield**, and **ambiguous**, showing decomposition, execution approach, validation, and trade-offs.

---

## Scenario 1: Greenfield - Build Core URL Shortener

### 1.1 Requirement Understanding
**Raw Requirement:**
> Build a URL shortener service that converts long URLs into 7-character short codes, tracks click counts, and provides analytics.

**Interpreted Intent:**
- Create REST API for shortening URLs and retrieving redirects
- Auto-generate unique short codes if no custom alias provided
- Track usage analytics (click count, access time)
- Support optional URL expiry
- Ensure data consistency across concurrent requests

**Ambiguities Identified & Resolved:**
- **Q:** Should short codes be human-readable?
  - **A:** Base62 alphanumeric (0-9A-Za-z) for 7-character length provides ~3.6 trillion combinations
- **Q:** What happens when a user requests an expired URL?
  - **A:** Return 410 Gone status; expired URLs remain reserved (no cleanup scheduled)
- **Q:** Should duplicate long URLs generate new short codes?
  - **A:** No; reuse existing short code to optimize storage and provide consistent URLs

### 1.2 Task Decomposition

| Task | Priority | Dependencies | Effort |
|------|----------|--------------|--------|
| Design data model (UrlMapping entity) | P0 | None | 0.5h |
| Implement JPA repository | P0 | Data model | 0.5h |
| Implement UrlShorteningService | P0 | Repository | 0.25h |
| Implement UrlRedirectService | P0 | Repository | 0.25h |
| Implement AnalyticsService | P0 | Repository | 0.25h |
| Create REST controllers | P0 | Services | 0.25h |
| Add DTOs & validation | P1 | Controllers | 0.25h |
| Write unit tests | P1 | Services | 0.5h |
| Database schema (Flyway) | P0 | Data model | 1h |
| Docker setup | P2 | Application | 1h |

### 1.3 Execution Approach

**Phase 1: Foundation (Hours 1-1.5)**
- Define `UrlMapping` entity with JPA annotations
- Create `UrlMappingRepository` interface extending `JpaRepository`
- Write Flyway migration `V1__create_url_mapping.sql`
- Verify with in-memory H2 during unit tests

**Phase 2: Business Logic (Hours 2-2.5)**
- Implement `UrlShorteningService`: auto-generate codes, detect duplicates, validate aliases
- Implement `UrlRedirectService`: resolve codes, increment clicks, check expiry
- Implement `AnalyticsService`: read-only stats retrieval
- Apply `@Transactional` to state-changing operations

**Phase 3: API Layer (Hours 1-1.5)**
- Create `UrlCreateController` (POST `/api/shorten`)
- Create `UrlRedirectController` (GET `/{shortCode}`)
- Create `AnalyticsController` (GET `/api/stats/{shortCode}`)
- Add DTOs with Jakarta Bean Validation annotations

**Phase 4: Testing & Deployment (Hours 2-3)**
- Write unit tests for service logic
- Create `application-test.properties` for H2
- Write integration tests with TestContainers (optional)
- Build Docker Compose file for local testing

### 1.4 AI-Assisted Execution

**Use of AI:**
- Generated initial entity structure and repository interface
- AI created CRUD service methods with null checks and error handling
- AI drafted REST controller endpoints with proper HTTP status codes
- AI generated unit test cases with parameterized tests
- AI created Flyway migration scripts with appropriate constraints

**Quality Gates Applied:**
1. **Compilation:** All code compiles without errors
2. **Unit Tests:** 100% service layer coverage
3. **Integration Tests:** End-to-end flow (create → redirect → stats)
4. **Code Review:** Verified against SOLID principles
5. **Performance:** No N+1 query issues (indexed columns)

**Output Ownership:**
- Engineer reviewed all generated code for correctness
- Engineer adjusted AI-generated tests to match domain logic
- Engineer validated error handling paths
- Engineer approved migration scripts before deployment

### 1.5 Validation

**Test Scenarios:**
```
✓ Create short URL → Verify 7-character code generated
✓ Redirect to short code → Verify 302 response to long URL
✓ Duplicate long URL → Verify same short code returned
✓ Custom alias → Verify custom code accepted if unique
✓ Expired URL → Verify 410 Gone on access
✓ Non-existent short code → Verify 404 Not Found
✓ Click count tracking → Verify incremented on each redirect
✓ Concurrent requests → Verify no race conditions
```

**Safety Checks:**
- Database constraints: UNIQUE on `short_code`, INDEX on `long_url`
- Application-level validation: URL format, alias format, expiry duration
- Exception handling: All invalid states throw domain exceptions (caught by GlobalExceptionHandler)

### Reliability & Concurrency Handling

To ensure reliability under concurrent access, the design uses a combination of database-level protections and transactional logic rather than relying on application-level assumptions alone.

**Key mechanisms:**
- **Unique database constraints** on `short_code` and `long_url` prevent duplicate inserts when two requests try to create the same short code or the same long URL at the same time.
- **Transactional redirect updates** ensure click counting and last-access timestamp changes happen as one atomic operation, avoiding partially updated state.
- **Repository-level atomic increment** updates the click count with a single query instead of read-then-write logic, which prevents lost-update problems in concurrent traffic.
- **Collision handling in short-code generation** retries when a generated code already exists, reducing risk of duplicate URLs under load.
- **Validation before persistence** rejects malformed inputs early, reducing bad writes and downstream failures.

This is important because URL shorteners are vulnerable to race conditions: two requests may try to create the same short code or increment the same click counter simultaneously. The application handles these cases in a database-safe way so the final state remains consistent and predictable even under concurrent traffic.

### 1.6 Assumptions & Limitations

**Assumptions:**
- All URLs are HTTP/HTTPS (no FTP, etc.)
- Short code generation is sufficient for uniqueness (collision probability ~0.0001 over 1M codes)
- Expiry is evaluated at access time (not scheduled cleanup)
- Single PostgreSQL instance (no multi-region replication)

**Limitations:**
- No rate limiting (future enhancement)
- No audit logging (future enhancement)
- No QR code generation (future enhancement)
- Expired URLs remain in database (storage cost; cleanup deferred)

---

## Scenario 2: Brownfield - Refactor for SOLID Principles

### 2.1 Requirement Understanding
**Raw Requirement:**
> Refactor the existing service layer to comply with SOLID principles, particularly SRP and DIP, while maintaining all existing functionality.

**Interpreted Intent:**
- Identify services with multiple responsibilities and split them
- Move low-level utilities (code generation, validation) to separate classes
- Introduce service interfaces to enable dependency inversion
- Maintain backward compatibility with existing APIs

**Ambiguities Identified & Resolved:**
- **Q:** Should we introduce interfaces if there's only one implementation?
  - **A:** Yes; DIP provides testability, future extensibility, and decoupling regardless of current implementations
- **Q:** How much should the facade abstract?
  - **A:** Minimize: facade only transforms results; business logic stays in services
- **Q:** Should we apply `@Override` annotations?
  - **A:** Yes; improves compile-time safety and code clarity

### 2.2 Task Decomposition

| Task | Priority | Dependencies | Effort |
|------|----------|--------------|--------|
| Extract ShortCodeGenerator utility | P1 | UrlShorteningService | 1h |
| Extract AliasValidator | P1 | UrlShorteningService | 1h |
| Extract LinkExpiryPolicy | P1 | UrlRedirectService | 1h |
| Create ShorteningService interface | P1 | UrlShorteningService | 1h |
| Create RedirectService interface | P1 | UrlRedirectService | 1h |
| Create AnalyticsService interface | P1 | UrlAnalyticsService | 1h |
| Update implementations to implement interfaces | P1 | Interfaces | 1h |
| Add @Override annotations | P1 | Implementations | 30m |
| Create UrlShortenerFacade | P1 | Interfaces | 1h |
| Update controllers to use facade | P1 | Facade | 1h |
| Update tests for new structure | P1 | Refactoring | 2h |
| Verify backward compatibility | P1 | All | 1h |

### 2.3 Execution Approach

**Phase 1: Identify Problems (Hours 1)**
- Review existing service layer for SRP violations
- Found: UrlShorteningService handling code generation, validation, shortening
- Found: UrlRedirectService handling expiry logic mixed with redirect logic

**Phase 2: Extract Utilities (Hours 2)**
- Move code generation to `ShortCodeGenerator` (util package)
- Move alias validation to `AliasValidator` (service package)
- Move expiry evaluation to `LinkExpiryPolicy` (service package)
- Each class has single, focused responsibility

**Phase 3: Introduce Interfaces (Hours 2)**
- Create `ShorteningService`, `RedirectService`, `AnalyticsService` interfaces
- Move result records to interfaces (e.g., `ShorteningService.ShortenResult`)
- Implement interfaces in concrete service classes
- Add `@Override` annotations for compile-time safety

**Phase 4: Facade & Decoupling (Hours 2)**
- Create `UrlShortenerFacade` that depends on interfaces
- Facade transforms service results to its own DTO records
- Update all controllers to inject facade instead of services
- Delete old direct service references

**Phase 5: Testing & Validation (Hours 1.5)**
- Run unit tests to verify behavior unchanged
- Update tests to mock interfaces (more flexible)
- Integration tests with facade API
- Verify Docker build still works

### 2.4 AI-Assisted Execution

**Use of AI:**
- AI created interface definitions with proper method signatures
- AI generated facade implementation with result transformation
- AI updated all class declarations to implement interfaces
- AI generated multi-file batch refactoring to update all references
- AI created test doubles for interface mocking

**Quality Gates Applied:**
1. **Compilation:** All code compiles without errors (critical: fixed import issues)
2. **Functionality:** All unit tests pass; behavior unchanged
3. **Design Review:** Verified SRP, DIP compliance
4. **Backward Compatibility:** Public API (facades & controllers) unchanged
5. **Code Coverage:** No regression in test coverage

**Output Ownership:**
- Engineer verified each service implements correct interface methods
- Engineer caught and fixed test import issues
- Engineer validated that facade correctly transforms results
- Engineer approved removal of duplicate ShortCodeGenerator class

### 2.5 Validation

**Refactoring Validation:**
```
✓ All existing tests pass (no behavior change)
✓ Interfaces correctly define service contracts
✓ Implementations have @Override annotations
✓ Facade properly transforms service results
✓ Controllers depend on facade, not services
✓ No circular dependencies
✓ No unused imports
✓ Build succeeds without compilation warnings
```

**Design Validation:**
```
✓ SRP: Each service handles one concern
✓ DIP: Controllers → Facade → Interfaces → Implementations
✓ OCP: New implementations can be added without changing controller code
✓ ISP: Interfaces are minimal (not fat)
✓ LSP: Not directly applicable (no polymorphic substitution yet)
```

**Risk Mitigation:**
- Incremental refactoring (one interface at a time)
- Comprehensive test coverage (unit & integration)
- Build validation after each major change
- Explicit dependency tracking (Spring autowiring)

### 2.6 Assumptions & Limitations

**Assumptions:**
- No production traffic during refactoring (safe change window)
- Existing test suite is comprehensive (changes caught early)
- Spring bean wiring automatically updates when implementations change

**Limitations:**
- Interfaces don't enable true LSP yet (only one implementation each)
- No adapter pattern (not needed yet; future: if multiple storage backends)
- Facade still delegates directly (no caching or retry logic)

---

## Scenario 3: Ambiguous - Add Alias Expiry with Reservation

### 3.1 Requirement Understanding
**Raw Requirement (Ambiguous):**
> Support custom URL aliases that don't expire like the main URL. Allow users to specify optional expiry durations. When a URL expires, the short code should stay reserved to prevent squatting.

**Interpreted Intent:**
- Allow custom aliases as alternative to auto-generated codes
- Both aliases and auto-generated codes should support optional expiry
- Expired URLs should not be deleted; the short code remains reserved (not reassigned)
- Accessing an expired URL returns 410 Gone (not 404)
- Future: background job to clean up ancient expired URLs (multi-week TTL)

**Ambiguities Identified & Resolved:**
- **Q:** What makes a valid alias? (Letters only? Numbers? Special characters?)
  - **A:** Alphanumeric, 4-32 characters (human-readable, URL-safe, no special chars)
- **Q:** Can aliases have different expiry from long URLs?
  - **A:** No; same URL with different aliases would have different expirations. Single expiry per UrlMapping.
- **Q:** How long to keep expired codes reserved?
  - **A:** Indefinitely (conservative). Future: 90-day auto-cleanup as asynchronous job.
- **Q:** Should we prevent alias reassignment after expiry?
  - **A:** Yes; reserved forever (or until cleanup). Prevents user confusion.
- **Q:** What HTTP status for expired URLs?
  - **A:** 410 Gone (permanent removal, not temporary 404)

### 3.2 Task Decomposition

| Task | Priority | Dependencies | Effort |
|------|----------|--------------|--------|
| Add expiresAt column to UrlMapping | P0 | Database | 1h |
| Create Flyway migration V2 (alter table) | P0 | UrlMapping | 1h |
| Implement LinkExpiryPolicy.isExpired() | P1 | UrlMapping | 1h |
| Implement LinkExpiryPolicy.resolveExpiresAt() | P1 | Policy | 1h |
| Add expiresInSeconds to ShortenRequest DTO | P1 | Policy | 30m |
| Update UrlShorteningService to apply expiry | P1 | Policy, DTO | 1h |
| Add UrlExpiredException handler | P1 | GlobalExceptionHandler | 1h |
| Update UrlRedirectService to check expiry | P1 | Policy | 1h |
| Add AliasValidator for custom aliases | P1 | Validation | 1h |
| Add validation to ShortenRequest (regex, size) | P1 | DTO | 1h |
| Write tests for expiry logic | P1 | Service | 2h |
| Write tests for alias validation | P1 | Service | 1h |
| Document expiry behavior in README | P2 | Implementation | 30m |

### 3.3 Execution Approach

**Phase 1: Data Model (Hours 1)**
- Add `expiresAt` (TIMESTAMP, nullable) to `UrlMapping`
- Create Flyway V2 migration: add column with default NULL
- Existing data (no expiry) grandfathered in

**Phase 2: Policy Implementation (Hours 1.5)**
- Implement `LinkExpiryPolicy.isExpired(Instant expiresAt, Instant now): boolean`
  - Returns `false` if `expiresAt == null` (no expiry)
  - Returns `true` if `now >= expiresAt`
- Implement `LinkExpiryPolicy.resolveExpiresAt(Long expiresInSeconds): Instant`
  - Returns `null` if `expiresInSeconds == null`
  - Returns `Instant.now() + Duration.ofSeconds(expiresInSeconds)`

**Phase 3: Shortening Flow (Hours 1.5)**
- Add `expiresInSeconds` field to `ShortenRequest` with `@Positive` validation
- Update `UrlShorteningService.shorten()` to call `linkExpiryPolicy.resolveExpiresAt()`
- Store `expiresAt` in newly created `UrlMapping`

**Phase 4: Alias Validation (Hours 1.5)**
- Implement `AliasValidator`
  - Reserved names: common aliases (admin, api, test, www, mail, etc.)
  - Character validation: `^[A-Za-z0-9]+$`
  - Length validation: 4-32 characters
- Add to `ShortenRequest` with `@Pattern` and `@Size` annotations
- Update `UrlShorteningService` to check for duplicate aliases

**Phase 5: Redirect & Exception Handling (Hours 1.5)**
- Update `UrlRedirectService.resolve()` to check expiry via policy
- Throw `UrlExpiredException` if expired
- Create `UrlExpiredException` exception class
- Add handler in `GlobalExceptionHandler` returning 410 Gone

**Phase 6: Testing & Validation (Hours 1.5)**
- Write tests: expiry evaluation (expired, not expired, no expiry)
- Write tests: alias validation (valid, invalid, reserved, duplicates)
- Write tests: end-to-end flow with and without expiry
- Database schema validation (migration runs cleanly)

### 3.4 AI-Assisted Execution

**Use of AI:**
- AI created Flyway migration script with proper NULL handling
- AI generated LinkExpiryPolicy class with temporal logic
- AI updated DTOs with validation annotations
- AI created comprehensive test cases covering edge cases
- AI generated GlobalExceptionHandler exception mappings
- AI created AliasValidator with reserved word list

**Quality Gates Applied:**
1. **Compilation:** All code compiles; import fixes applied
2. **Unit Tests:** 100% coverage of expiry and validation logic
3. **Integration Tests:** End-to-end create/redirect with expiry
4. **Database:** Migration runs on fresh schema and existing schema
5. **API Contract:** Backward compatible (expiresInSeconds and customAlias optional)
6. **Error Handling:** Proper exception mapping to HTTP status codes

**Output Ownership:**
- Engineer verified expiry logic for corner cases (null, zero, far-future)
- Engineer reviewed Flyway migration for data loss risks (none)
- Engineer tested backward compatibility (old data without expiry)
- Engineer validated error responses (410 Gone vs 404 Not Found distinction)
- Engineer approved reserved alias list against real-world conflicts

### 3.5 Validation

**Functional Testing:**
```
✓ Create URL with no expiry → Access unlimited times
✓ Create URL with 1-hour expiry → Access within 1 hour succeeds
✓ Create URL with expired → Access after expiry throws UrlExpiredException (410)
✓ Custom alias "abc1234" → Created successfully
✓ Custom alias "admin" → Rejected (reserved)
✓ Custom alias "ab" → Rejected (too short)
✓ Custom alias "abc_def" → Rejected (special char)
✓ Duplicate custom alias → Rejected (DuplicateAliasException)
✓ Old data without expiry → Still accessible (backward compatible)
```

**Edge Cases:**
```
✓ expiresInSeconds = 0 → Expires immediately (caught by @Positive validation)
✓ expiresInSeconds = very large number → Far future expiry (no overflow)
✓ expiresAt = null → Never expires (treated as no TTL)
✓ Access at exact expiry boundary → Considered expired (>=, not >)
✓ Custom alias with mixed case (AbC123) → Normalized and stored
```

**Error Handling:**
```
✓ 400 Bad Request: Invalid expiresInSeconds (not positive)
✓ 400 Bad Request: Invalid customAlias (bad format or too long)
✓ 409 Conflict: Duplicate customAlias
✓ 410 Gone: Expired URL access
✓ 404 Not Found: Non-existent short code
```

### 3.6 Assumptions & Limitations

**Assumptions:**
- Aliases are case-insensitive for user intent but stored as-is (future: normalize to lowercase)
- Expiry is evaluated at access time; no background cleanup scheduled
- Reserved alias list is static (future: admin-configurable)
- System clock is synchronized (no time-skew issues assumed)

**Limitations:**
- No granular expiry policies (all URLs use same expiry mechanism)
- No callback on expiry (async event publishing not implemented)
- No soft-delete (expired URLs remain in DB forever)
- Reserved aliases can't be repurposed later
- No retry logic for concurrent alias creation attempts

**Future Enhancements:**
1. **Async Cleanup:** Background job to mark/archive old expired URLs (>90 days)
2. **Admin UI:** Manage reserved aliases and cleanup policies
3. **Webhook:** Notify user app when URL expires (callback mechanism)
4. **Audit Trail:** Log all alias reservations and expirations
5. **Soft Delete:** Move expired URLs to archive table instead of leaving in prod table

### 3.7 Design Trade-offs

| Decision | Pro | Con |
|----------|-----|-----|
| Expiry at access time (not scheduled) | Simple, no background jobs | Stale data remains in DB |
| Permanent reservation of short codes | Prevent squatting, user confusion | DB bloat over time |
| Static reserved alias list | Fast lookup, deterministic | Manual updates needed |
| Single expiry per UrlMapping | No conflict, simple model | Can't have alias + URL with different TTLs |
| 410 Gone vs 404 Not Found | Correct HTTP semantics | Reveals URL expired to client |

---

## Summary

This document demonstrates three engineering scenarios:

1. **Greenfield:** Building core URL shortener from first principles, with full decomposition, task sequencing, and validation
2. **Brownfield:** Refactoring for SOLID principles while maintaining compatibility, with design validation
3. **Ambiguous:** Clarifying vague requirements (alias + expiry), resolving conflicts, and documenting trade-offs

Each scenario shows:
- **Requirement clarity** (interpreted intent, ambiguities resolved)
- **Task breakdown** (priorities, dependencies, effort estimates)
- **Execution strategy** (phases, decision points, quality gates)
- **AI partnership** (where AI assisted, quality checkpoints, engineer ownership)
- **Validation** (test cases, edge cases, error scenarios)
- **Assumptions & limitations** (trade-offs, future enhancements)

The URL Shortener evolved through careful engineering judgment, balancing feature completeness, architectural cleanliness, testability, and maintainability.
