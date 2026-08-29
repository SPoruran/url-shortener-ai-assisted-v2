package com.schwab.urlshortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.service.UrlShortenerService;

@SpringBootTest
class UrlShortenerServiceTest {

    @Autowired
    private UrlShortenerService service;

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
}
