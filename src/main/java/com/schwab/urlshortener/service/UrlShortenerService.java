package com.schwab.urlshortener.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.exception.UrlExpiredException;
import com.schwab.urlshortener.exception.DuplicateAliasException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;

@Service
public class UrlShortenerService {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,32}$");
    private static final Set<String> RESERVED_ALIASES = Set.of("api", "health", "shorten", "stats");

    private final SecureRandom random = new SecureRandom();
    private final UrlMappingRepository urlMappingRepository;

    public UrlShortenerService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    public ShortenResult shorten(String longUrl) {
        return shorten(longUrl, null, null);
    }

    public ShortenResult shorten(String longUrl, String customAlias, Long expiresInSeconds) {
        Optional<UrlMapping> existingByLongUrl = urlMappingRepository.findByLongUrl(longUrl);
        if (existingByLongUrl.isPresent()) {
            UrlMapping existing = existingByLongUrl.get();
            return new ShortenResult(existing.getShortCode(), existing.getLongUrl());
        }

        String shortCode = resolveShortCode(customAlias);
        Instant expiresAt = resolveExpiresAt(expiresInSeconds);
        UrlMapping mapping = new UrlMapping(shortCode, longUrl, Instant.now(), expiresAt);
        urlMappingRepository.save(mapping);
        return new ShortenResult(shortCode, longUrl);
    }

    @Transactional
    public String resolve(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        Instant now = Instant.now();
        if (mapping.getExpiresAt() != null && now.isAfter(mapping.getExpiresAt())) {
            throw new UrlExpiredException(shortCode);
        }

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

    private String resolveShortCode(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return generateUniqueCode();
        }

        String normalizedAlias = customAlias.trim();
        validateCustomAlias(normalizedAlias);

        if (urlMappingRepository.findByShortCode(normalizedAlias).isPresent()) {
            throw new DuplicateAliasException(normalizedAlias);
        }

        return normalizedAlias;
    }

    private void validateCustomAlias(String customAlias) {
        if (!CUSTOM_ALIAS_PATTERN.matcher(customAlias).matches()) {
            throw new IllegalArgumentException("customAlias must be 4-32 characters long and contain only letters or numbers");
        }

        String normalized = customAlias.toLowerCase(Locale.ROOT);
        if (RESERVED_ALIASES.contains(normalized)) {
            throw new IllegalArgumentException("customAlias is reserved and cannot be used");
        }
    }

    private Instant resolveExpiresAt(Long expiresInSeconds) {
        if (expiresInSeconds == null) {
            return null;
        }
        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException("expiresInSeconds must be greater than 0 when provided");
        }
        return Instant.now().plusSeconds(expiresInSeconds);
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
