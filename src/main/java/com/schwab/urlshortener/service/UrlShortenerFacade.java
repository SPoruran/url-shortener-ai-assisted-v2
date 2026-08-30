package com.schwab.urlshortener.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class UrlShortenerFacade {

    private final ShorteningService shorteningService;
    private final RedirectService redirectService;
    private final AnalyticsService analyticsService;

    public UrlShortenerFacade(
            ShorteningService shorteningService,
            RedirectService redirectService,
            AnalyticsService analyticsService) {
        this.shorteningService = shorteningService;
        this.redirectService = redirectService;
        this.analyticsService = analyticsService;
    }

    public ShortenResult shorten(String longUrl) {
        ShorteningService.ShortenResult result = shorteningService.shorten(longUrl);
        return new ShortenResult(result.shortCode(), result.longUrl());
    }

    public ShortenResult shorten(String longUrl, String customAlias, Long expiresInSeconds) {
        ShorteningService.ShortenResult result = shorteningService.shorten(longUrl, customAlias, expiresInSeconds);
        return new ShortenResult(result.shortCode(), result.longUrl());
    }

    public String resolve(String shortCode) {
        return redirectService.resolve(shortCode);
    }

    public UrlStats getStats(String shortCode) {
        AnalyticsService.UrlStats stats = analyticsService.getStats(shortCode);
        return new UrlStats(stats.shortCode(), stats.longUrl(), stats.clickCount(), stats.createdAt(), stats.lastAccessedAt());
    }

    public record ShortenResult(String shortCode, String longUrl) {
    }

    public record UrlStats(String shortCode, String longUrl, long clickCount, Instant createdAt, Instant lastAccessedAt) {
    }
}
