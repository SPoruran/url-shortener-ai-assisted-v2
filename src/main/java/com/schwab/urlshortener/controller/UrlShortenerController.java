package com.schwab.urlshortener.controller;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.schwab.urlshortener.dto.ShortenRequest;
import com.schwab.urlshortener.dto.ShortenResponse;
import com.schwab.urlshortener.dto.StatsResponse;
import com.schwab.urlshortener.service.UrlShortenerService;

import jakarta.validation.Valid;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        UrlShortenerService.ShortenResult result = urlShortenerService.shorten(request.getLongUrl());
        String shortUrl = "/" + result.shortCode();
        ShortenResponse response = new ShortenResponse(result.shortCode(), shortUrl, result.longUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = urlShortenerService.resolve(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .build();
    }

    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<StatsResponse> stats(@PathVariable String shortCode) {
        UrlShortenerService.UrlStats stats = urlShortenerService.getStats(shortCode);
        StatsResponse response = new StatsResponse(
                stats.shortCode(),
                stats.longUrl(),
                stats.clickCount(),
                stats.createdAt(),
                stats.lastAccessedAt()
        );
        return ResponseEntity.ok(response);
    }
}
