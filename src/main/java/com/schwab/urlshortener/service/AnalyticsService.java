package com.schwab.urlshortener.service;

import java.time.Instant;

public interface AnalyticsService {
    UrlStats getStats(String shortCode);

    record UrlStats(String shortCode, String longUrl, long clickCount, Instant createdAt, Instant lastAccessedAt) {
    }
}
