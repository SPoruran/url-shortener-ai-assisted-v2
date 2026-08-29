package com.schwab.urlshortener.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;

@Service
public class UrlShortenerService {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final UrlMappingRepository urlMappingRepository;

    public UrlShortenerService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    public ShortenResult shorten(String longUrl) {
        Optional<UrlMapping> existingByLongUrl = urlMappingRepository.findByLongUrl(longUrl);
        if (existingByLongUrl.isPresent()) {
            UrlMapping existing = existingByLongUrl.get();
            return new ShortenResult(existing.getShortCode(), existing.getLongUrl());
        }

        String shortCode = generateUniqueCode();
        UrlMapping mapping = new UrlMapping(shortCode, longUrl, Instant.now());
        urlMappingRepository.save(mapping);
        return new ShortenResult(shortCode, longUrl);
    }

    @Transactional
    public String resolve(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        Instant now = Instant.now();
        urlMappingRepository.incrementClickCount(shortCode, now);
        return mapping.getLongUrl();
    }

    public UrlStats getStats(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        return new UrlStats(
                mapping.getShortCode(),
                mapping.getLongUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt(),
                mapping.getLastAccessedAt()
        );
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = randomCode();
            if (urlMappingRepository.findByShortCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique short code after "
                + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public record ShortenResult(String shortCode, String longUrl) {
    }

    public record UrlStats(String shortCode, String longUrl, long clickCount, Instant createdAt, Instant lastAccessedAt) {
    }
}
