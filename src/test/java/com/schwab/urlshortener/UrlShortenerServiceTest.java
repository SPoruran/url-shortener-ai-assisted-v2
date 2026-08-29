package com.schwab.urlshortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.schwab.urlshortener.exception.DuplicateAliasException;
import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.exception.UrlExpiredException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;
import com.schwab.urlshortener.service.UrlShortenerService;

@SpringBootTest
class UrlShortenerServiceTest {

    @Autowired
    private UrlShortenerService service;

    @Autowired
    private UrlMappingRepository repository;

    @Test
    void shorten_returnsNonBlankCodeAndPreservesLongUrl() {
        var result = service.shorten("https://example.com/some/very/long/path");

        assertNotNull(result.shortCode());
        assertFalse(result.shortCode().isBlank());
        assertEquals("https://example.com/some/very/long/path", result.longUrl());
    }

    @Test
    void resolve_returnsOriginalUrlForKnownCode() {
        var result = service.shorten("https://example.com/foo");

        String resolved = service.resolve(result.shortCode());

        assertEquals("https://example.com/foo", resolved);
    }

    @Test
    void resolve_throwsForUnknownCode() {
        assertThrows(ShortCodeNotFoundException.class, () -> service.resolve("doesNotExist"));
    }

    @Test
    void shorten_generatesDifferentCodesForDifferentCalls() {
        var first = service.shorten("https://example.com/a");
        var second = service.shorten("https://example.com/b");

        assertNotEquals(first.shortCode(), second.shortCode());
    }

    @Test
    void shorten_reusesExistingCodeForDuplicateLongUrl() {
        var first = service.shorten("https://example.com/duplicate");
        var second = service.shorten("https://example.com/duplicate");

        assertEquals(first.shortCode(), second.shortCode());
        assertEquals(first.longUrl(), second.longUrl());
    }

    @Test
    void resolve_incrementsClickCountAndUpdatesLastAccessedAt() {
        var result = service.shorten("https://example.com/analytics");

        String resolved = service.resolve(result.shortCode());
        var stats = service.getStats(result.shortCode());

        assertEquals("https://example.com/analytics", resolved);
        assertEquals(1L, stats.clickCount());
        assertNotNull(stats.lastAccessedAt());
    }

    @Test
    void getStats_throwsForUnknownCode() {
        assertThrows(ShortCodeNotFoundException.class, () -> service.getStats("doesNotExist"));
    }

    @Test
    void shorten_acceptsCustomAlias() {
        var result = service.shorten("https://example.com/custom-alias", "custom42", null);

        assertEquals("custom42", result.shortCode());
        assertEquals("https://example.com/custom-alias", result.longUrl());
    }

    @Test
    void shorten_rejectsDuplicateCustomAlias() {
        service.shorten("https://example.com/first", "takenAlias", null);

        assertThrows(DuplicateAliasException.class,
                () -> service.shorten("https://example.com/second", "takenAlias", null));
    }

    @Test
    void shorten_rejectsInvalidCustomAliasCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> service.shorten("https://example.com/bad", "bad_alias!", null));
    }

    @Test
    void shorten_rejectsReservedCustomAlias() {
        assertThrows(IllegalArgumentException.class,
                () -> service.shorten("https://example.com/reserved", "api", null));
    }

    @Test
    void shorten_withoutCustomAlias_stillGeneratesCode() {
        var result = service.shorten("https://example.com/auto-generation");

        assertNotNull(result.shortCode());
        assertFalse(result.shortCode().isBlank());
        assertEquals("https://example.com/auto-generation", result.longUrl());
    }

    @Test
    void resolve_throwsWhenLinkIsExpired() {
        repository.save(new UrlMapping("expiredLink", "https://example.com/expired", Instant.now().minusSeconds(120), Instant.now().minusSeconds(60)));

        assertThrows(UrlExpiredException.class, () -> service.resolve("expiredLink"));
    }

    @Test
    void shorten_withExpiryAndCustomAlias_works() {
        var result = service.shorten("https://example.com/expiring", "expiringLink", 60L);

        assertEquals("expiringLink", result.shortCode());
        assertEquals("https://example.com/expiring", result.longUrl());
        assertNotNull(service.getStats("expiringLink").createdAt());
    }

    @Test
    void shorten_withoutExpiry_doesNotExpire() {
        var result = service.shorten("https://example.com/no-expiry", "noExpiryLink", null);

        assertEquals("noExpiryLink", result.shortCode());
        assertEquals("https://example.com/no-expiry", service.resolve(result.shortCode()));
    }
}
