package com.schwab.urlshortener.dto;

import java.time.Instant;

public class StatsResponse {

    private String shortCode;
    private String longUrl;
    private long clickCount;
    private Instant createdAt;
    private Instant lastAccessedAt;

    public StatsResponse(String shortCode, String longUrl, long clickCount, Instant createdAt, Instant lastAccessedAt) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
}
