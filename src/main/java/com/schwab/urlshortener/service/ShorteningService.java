package com.schwab.urlshortener.service;

public interface ShorteningService {
    ShortenResult shorten(String longUrl);

    ShortenResult shorten(String longUrl, String customAlias, Long expiresInSeconds);

    record ShortenResult(String shortCode, String longUrl) {
    }
}
