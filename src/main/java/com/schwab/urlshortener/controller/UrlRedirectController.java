package com.schwab.urlshortener.controller;

import java.net.URI;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.schwab.urlshortener.service.UrlShortenerFacade;

@RestController
public class UrlRedirectController {

    private final UrlShortenerFacade urlShortenerFacade;

    public UrlRedirectController(UrlShortenerFacade urlShortenerFacade) {
        this.urlShortenerFacade = urlShortenerFacade;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = urlShortenerFacade.resolve(shortCode);
        URI redirectUri = URI.create(Objects.requireNonNull(longUrl, "Resolved URL cannot be null"));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .build();
    }
}
