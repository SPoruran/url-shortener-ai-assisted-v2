package com.schwab.urlshortener.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.exception.UrlExpiredException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;

@Service
public class UrlRedirectService implements RedirectService {

    private final UrlMappingRepository urlMappingRepository;
    private final LinkExpiryPolicy linkExpiryPolicy;

    public UrlRedirectService(UrlMappingRepository urlMappingRepository, LinkExpiryPolicy linkExpiryPolicy) {
        this.urlMappingRepository = urlMappingRepository;
        this.linkExpiryPolicy = linkExpiryPolicy;
    }

    @Override
    @Transactional
    public String resolve(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        Instant now = Instant.now();
        if (linkExpiryPolicy.isExpired(mapping.getExpiresAt(), now)) {
            throw new UrlExpiredException(shortCode);
        }

        urlMappingRepository.incrementClickCount(shortCode, now);
        return mapping.getLongUrl();
    }
}
