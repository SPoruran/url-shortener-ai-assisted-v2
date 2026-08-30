package com.schwab.urlshortener;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.schwab.urlshortener.exception.DuplicateAliasException;
import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.exception.UrlExpiredException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;
import com.schwab.urlshortener.service.UrlAnalyticsService;
import com.schwab.urlshortener.service.UrlRedirectService;
import com.schwab.urlshortener.service.UrlShorteningService;

@SpringBootTest
class ShortUrlServiceTest {

    @Autowired
    private UrlShorteningService shorteningService;

    @Autowired
    private UrlRedirectService redirectService;

    @Autowired
    private UrlAnalyticsService analyticsService;

    @Autowired
    private UrlMappingRepository repository;

    @Test
    void shorten_returnsNonBlankCodeAndPreservesLongUrl() {
        var result = shorteningService.shorten("https://example.com/some/very/long/path");

        assertNotNull(result.shortCode());
        assertFalse(result.shortCode().isBlank());
        assertEquals("https://example.com/some/very/long/path", result.longUrl());
    }

    @Test
    void resolve_returnsOriginalUrlForKnownCode() {
        var result = shorteningService.shorten("https://example.com/foo");

        String resolved = redirectService.resolve(result.shortCode());

        assertEquals("https://example.com/foo", resolved);
    }

    @Test
    void resolve_throwsForUnknownCode() {
        assertThrows(ShortCodeNotFoundException.class, () -> redirectService.resolve("doesNotExist"));
    }

    @Test
    void shorten_generatesDifferentCodesForDifferentCalls() {
        var first = shorteningService.shorten("https://example.com/a");
        var second = shorteningService.shorten("https://example.com/b");

        assertNotEquals(first.shortCode(), second.shortCode());
    }

    @Test
    void shorten_reusesExistingCodeForDuplicateLongUrl() {
        var first = shorteningService.shorten("https://example.com/duplicate");
        var second = shorteningService.shorten("https://example.com/duplicate");

        assertEquals(first.shortCode(), second.shortCode());
        assertEquals(first.longUrl(), second.longUrl());
    }

    @Test
    void resolve_incrementsClickCountAndUpdatesLastAccessedAt() {
        var result = shorteningService.shorten("https://example.com/analytics");

        String resolved = redirectService.resolve(result.shortCode());
        var stats = analyticsService.getStats(result.shortCode());

        assertEquals("https://example.com/analytics", resolved);
        assertEquals(1L, stats.clickCount());
        assertNotNull(stats.lastAccessedAt());
    }

    @Test
    void getStats_throwsForUnknownCode() {
        assertThrows(ShortCodeNotFoundException.class, () -> analyticsService.getStats("doesNotExist"));
    }

    @Test
    void shorten_acceptsCustomAlias() {
        var result = shorteningService.shorten("https://example.com/custom-alias", "custom42", null);

        assertEquals("custom42", result.shortCode());
        assertEquals("https://example.com/custom-alias", result.longUrl());
    }

    @Test
    void shorten_rejectsDuplicateCustomAlias() {
        shorteningService.shorten("https://example.com/first", "takenAlias", null);

        assertThrows(DuplicateAliasException.class,
                () -> shorteningService.shorten("https://example.com/second", "takenAlias", null));
    }

    @Test
    void shorten_rejectsInvalidCustomAliasCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> shorteningService.shorten("https://example.com/bad", "bad_alias!", null));
    }

    @Test
    void shorten_rejectsReservedCustomAlias() {
        assertThrows(IllegalArgumentException.class,
                () -> shorteningService.shorten("https://example.com/reserved", "api", null));
    }

    @Test
    void shorten_withoutCustomAlias_stillGeneratesCode() {
        var result = shorteningService.shorten("https://example.com/auto-generation");

        assertNotNull(result.shortCode());
        assertFalse(result.shortCode().isBlank());
        assertEquals("https://example.com/auto-generation", result.longUrl());
    }

    @Test
    void resolve_throwsWhenLinkIsExpired() {
        repository.save(new UrlMapping("expiredLink", "https://example.com/expired", Instant.now().minusSeconds(120), Instant.now().minusSeconds(60)));

        assertThrows(UrlExpiredException.class, () -> redirectService.resolve("expiredLink"));
    }

    @Test
    void shorten_withExpiryAndCustomAlias_works() {
        var result = shorteningService.shorten("https://example.com/expiring", "expiringLink", 60L);

        assertEquals("expiringLink", result.shortCode());
        assertEquals("https://example.com/expiring", result.longUrl());
        assertNotNull(analyticsService.getStats("expiringLink").createdAt());
    }

    @Test
    void shorten_withoutExpiry_doesNotExpire() {
        var result = shorteningService.shorten("https://example.com/no-expiry", "noExpiryLink", null);

        assertEquals("noExpiryLink", result.shortCode());
        assertEquals("https://example.com/no-expiry", redirectService.resolve(result.shortCode()));
    }
}
