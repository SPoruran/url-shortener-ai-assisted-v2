package com.schwab.urlshortener.model;

import java.time.Instant;

/**
 * In-memory representation of a single short-code -> long-URL mapping.
 *
 * Iteration 1: no persistence, no expiry, no click tracking.
 * These are intentionally deferred to later iterations (see ITERATIONS.md).
 */
public class UrlMapping {

    private final String shortCode;
    private final String longUrl;
    private final Instant createdAt;

    public UrlMapping(String shortCode, String longUrl, Instant createdAt) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
