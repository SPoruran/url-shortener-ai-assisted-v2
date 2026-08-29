package com.schwab.urlshortener.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.model.UrlMapping;

/**
 * Iteration 1 - basic functionality only:
 *   - generate a short, unique, URL-safe code for a given long URL
 *   - resolve a short code back to its original long URL
 *
 * Storage: in-memory ConcurrentHashMap only. No database, no persistence
 * across restarts, no expiry, no analytics. These are explicitly out of
 * scope for this iteration - see ITERATIONS.md for the planned roadmap.
 */
@Service
public class UrlShortenerService {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();

    // shortCode -> UrlMapping
    private final Map<String, UrlMapping> codeToMapping = new ConcurrentHashMap<>();

    public ShortenResult shorten(String longUrl) {
        String shortCode = generateUniqueCode();
        UrlMapping mapping = new UrlMapping(shortCode, longUrl, Instant.now());
        codeToMapping.put(shortCode, mapping);
        return new ShortenResult(shortCode, longUrl);
    }

    public String resolve(String shortCode) {
        UrlMapping mapping = codeToMapping.get(shortCode);
        if (mapping == null) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        return mapping.getLongUrl();
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = randomCode();
            if (!codeToMapping.containsKey(candidate)) {
                return candidate;
            }
        }
        // Extremely unlikely with a 62^7 keyspace, but fail loudly rather than
        // silently overwrite an existing mapping.
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

    /**
     * Simple result carrier so the controller doesn't need to know about
     * the internal UrlMapping model.
     */
    public record ShortenResult(String shortCode, String longUrl) {
    }
}
