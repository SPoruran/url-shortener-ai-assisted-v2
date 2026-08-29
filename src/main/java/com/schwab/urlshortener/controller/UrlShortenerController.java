package com.schwab.urlshortener.controller;

import com.schwab.urlshortener.dto.ShortenRequest;
import com.schwab.urlshortener.dto.ShortenResponse;
import com.schwab.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Iteration 1 endpoints only:
 *   POST /api/shorten   - create a short code for a long URL
 *   GET  /{shortCode}   - redirect to the original long URL
 */
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
}
