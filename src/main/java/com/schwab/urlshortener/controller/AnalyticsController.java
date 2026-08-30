package com.schwab.urlshortener.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.schwab.urlshortener.dto.StatsResponse;
import com.schwab.urlshortener.service.UrlShortenerFacade;

@RestController
public class AnalyticsController {

    private final UrlShortenerFacade urlShortenerFacade;

    public AnalyticsController(UrlShortenerFacade urlShortenerFacade) {
        this.urlShortenerFacade = urlShortenerFacade;
    }

    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<StatsResponse> stats(@PathVariable String shortCode) {
        UrlShortenerFacade.UrlStats stats = urlShortenerFacade.getStats(shortCode);
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
