package com.schwab.urlshortener.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.schwab.urlshortener.exception.DuplicateAliasException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;
import com.schwab.urlshortener.util.ShortCodeGenerator;

@Service
public class UrlShorteningService implements ShorteningService {

    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final UrlMappingRepository urlMappingRepository;
    private final AliasValidator aliasValidator;
    private final LinkExpiryPolicy linkExpiryPolicy;

    public UrlShorteningService(UrlMappingRepository urlMappingRepository, AliasValidator aliasValidator, LinkExpiryPolicy linkExpiryPolicy) {
        this.urlMappingRepository = urlMappingRepository;
        this.aliasValidator = aliasValidator;
        this.linkExpiryPolicy = linkExpiryPolicy;
    }

    @Override
    public ShortenResult shorten(String longUrl) {
        return shorten(longUrl, null, null);
    }

    @Override
    public ShortenResult shorten(String longUrl, String customAlias, Long expiresInSeconds) {
        Optional<UrlMapping> existingByLongUrl = urlMappingRepository.findByLongUrl(longUrl);
        if (existingByLongUrl.isPresent()) {
            UrlMapping existing = existingByLongUrl.get();
            return new ShortenResult(existing.getShortCode(), existing.getLongUrl());
        }

        String shortCode = resolveShortCode(customAlias);
        Instant expiresAt = linkExpiryPolicy.resolveExpiresAt(expiresInSeconds);
        UrlMapping mapping = new UrlMapping(shortCode, longUrl, Instant.now(), expiresAt);
        urlMappingRepository.save(mapping);
        return new ShortenResult(shortCode, longUrl);
    }

    private String resolveShortCode(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return generateUniqueCode();
        }

        String normalizedAlias = aliasValidator.validateAndNormalize(customAlias);

        if (urlMappingRepository.findByShortCode(normalizedAlias).isPresent()) {
            throw new DuplicateAliasException(normalizedAlias);
        }

        return normalizedAlias;
    }

    private String generateUniqueCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = ShortCodeGenerator.generate(random);
            if (urlMappingRepository.findByShortCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique short code after "
                + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
