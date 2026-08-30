package com.schwab.urlshortener.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class LinkExpiryPolicy {

    public Instant resolveExpiresAt(Long expiresInSeconds) {
        if (expiresInSeconds == null) {
            return null;
        }
        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException("expiresInSeconds must be greater than 0 when provided");
        }
        return Instant.now().plusSeconds(expiresInSeconds);
    }

    public boolean isExpired(Instant expiresAt, Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}
