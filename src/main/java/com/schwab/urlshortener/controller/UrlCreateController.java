package com.schwab.urlshortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.schwab.urlshortener.dto.ShortenRequest;
import com.schwab.urlshortener.dto.ShortenResponse;
import com.schwab.urlshortener.service.UrlShortenerFacade;

import jakarta.validation.Valid;

@RestController
public class UrlCreateController {

    private final UrlShortenerFacade urlShortenerFacade;

    public UrlCreateController(UrlShortenerFacade urlShortenerFacade) {
        this.urlShortenerFacade = urlShortenerFacade;
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        UrlShortenerFacade.ShortenResult result = urlShortenerFacade.shorten(
                request.getLongUrl(),
                request.getCustomAlias(),
                request.getExpiresInSeconds()
        );
        String shortUrl = "/" + result.shortCode();
        ShortenResponse response = new ShortenResponse(result.shortCode(), shortUrl, result.longUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
