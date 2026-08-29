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

## Iteration 3 — <title>

**Date:**

**Goal:**

**Added:**

**Changed:**

**AI-assistance notes:**

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
