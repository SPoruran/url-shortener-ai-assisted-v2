# Iteration Log

Running log of each pass on this project. One entry per iteration, added as
I build

---

## Iteration 1 — Basic functionality (baseline)

**Date:** 2026-08-29 19:31:15

**Goal:** Implement only the primary requirement — accept a long URL,
return a unique short code, redirect on lookup. Nothing else.

**Added:**
- `POST /api/shorten` and `GET /{shortCode}` endpoints
- In-memory storage (`ConcurrentHashMap`)
- Base62, 7-character unique code generation with collision retry
- Basic request validation (`@Pattern` for http/https URLs)
- Centralized error handling (404 for unknown codes, 400 for bad input)
- Basic service-layer unit tests

**Explicitly deferred:** persistence, custom codes, expiry, analytics,
auth, Docker Compose, rate limiting.

**AI-assistance notes:** Used AI to generate a basic Spring Boot template 
via Spring Initializer with core dependencies. Built out the REST endpoints, 
in-memory storage, Base62 code generator, and validation layer from this foundation.

---

## Iteration 2 — PostgreSQL persistence + duplicate URL detection

**Date:** 2026-08-29 21:18:00

**Goal:** Replace the in-memory map with PostgreSQL persistence and ensure duplicate long URLs reuse the existing short code instead of creating a second record. Keep the existing API contract stable while upgrading the storage layer in a brownfield way.

**Added:**
- PostgreSQL datasource configuration for local development
- Docker-based Postgres service setup for the project
- JPA entity/repository layer for persisted short links
- Duplicate long-URL detection and reuse logic
- Migration-friendly repository abstraction that can coexist with the existing service API

**Changed:**
- `UrlShortenerService` will stop depending on the in-memory `ConcurrentHashMap`
- `UrlMapping` becomes a persisted entity with a database-backed primary key and timestamps
- `shorten()` should first check whether the long URL already exists, then return the existing short code rather than creating a duplicate
- The app configuration will add `spring.datasource.*` properties and local Postgres connection settings
- Future run instructions will include Docker startup for the Postgres container

**AI-assistance notes:** This is the brownfield upgrade step: keep the iteration-1 API working while swapping out storage under it. The duplicate matching requirement is critical because identical long URLs should resolve to the same short code, while unique URLs should still create a new short code and persist it to Postgres.

---

## Iteration 3 — Ambiguous scenario: analytics for link usage

**Date:** 2026-08-29 21:40:00

**Goal:** Scope the vague stakeholder request "Add analytics to the URL shortener" before implementation. Define a minimal, defensible analytics feature for this project without expanding into a full product-analytics platform.

**Added:**
- A formal analytics scope decision document: `ANALYTICS_DECISION.md`
- Explicit requirement clarification for click tracking and last-accessed timestamping
- Minimal iteration-3 analysis of what analytics means in this project
- A list of excluded analytics features that were intentionally deferred

**Changed:**
- The project now defines analytics as: successful redirect count + last access timestamp only
- Model changes will be limited to persisted link metadata (`clickCount`, `lastAccessedAt`)
- The work is intentionally constrained to a simple read endpoint for stats rather than broad reporting
- Concurrency handling is explicitly addressed with atomic increment semantics to avoid lost increments under load

**AI-assistance notes:** The stakeholder request was ambiguous enough to justify a scoping pass before code. The AI-generated direction was used to identify the ambiguity, define a narrow interpretation, and document the exclusions. The final decision was adjusted by hand to keep the scope honest: no PII, no external analytics, no geographic/device breakdowns, no dashboards, no cohort tracking.

**Explicitly deferred:** referrer tracking, geolocation, browser/device analytics, cohort reporting, dashboards, external platforms, and any other analytics feature that would require broader product and data-privacy decisions.

---

## Bugfix — Brownfield schema migration issue: `click_count` not null on existing rows

**Date:** 2026-08-29 16:22:55

**Problem:** Attempting to add a non-null `click_count` column via Hibernate auto-update caused PostgreSQL to reject the migration.

**Observed failure:**
- `HHH000489: No JTA platform available` appeared as a warning, but it was not the root cause.
- The actual blocker was: `column "click_count" of relation "url_mapping" contains null values` while executing `alter table if exists url_mapping add column click_count bigint not null`.

**Root cause:**
- The project had existing rows in `url_mapping` before the new `click_count` field was introduced.
- Hibernate `ddl-auto=update` tried to add a not-null column without a safe default for existing records.
- PostgreSQL refused the schema alteration because the new column was required for existing rows.

**Resolution:**
- Switched schema management from Hibernate auto-update to explicit Flyway migrations.
- Added a migration script to create `url_mapping` with a safe default for `click_count` (`0`) and an optional `last_accessed_at` field.
- Kept `ddl-auto=none` to prevent unsafe in-place schema mutation in future brownfield changes.
- If the local database is already stuck in a bad schema state, rebuild it with:

```bash
mvn flyway:clean flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/urlshortener \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres \
  -Dflyway.cleanDisabled=false
```

**Changed files:**
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/resources/db/migration/V1__init_schema.sql`

**AI-assistance notes:** The fix was driven by the actual PostgreSQL error rather than a guess. The root cause was the migration strategy, not the JTA warning, so the change focused on safe database evolution and explicit schema management.

---

<!-- Copy the block above for each new iteration. Suggested future entries:
     - Persistence (Spring Data / MongoDB or Postgres) replacing the in-memory map
     - Docker Compose (app + DB)
     - Custom/vanity short codes
     - Link expiry (TTL)
     - Click analytics
     - Rate limiting
     - Auth / per-user link ownership
-->
