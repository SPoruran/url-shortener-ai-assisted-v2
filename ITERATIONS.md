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

## Iteration 2 — <title>

**Date:**

**Goal:**

**Added:**

**Changed:**

**AI-assistance notes:**

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
