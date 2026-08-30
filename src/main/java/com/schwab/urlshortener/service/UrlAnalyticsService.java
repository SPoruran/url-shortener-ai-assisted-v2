package com.schwab.urlshortener.service;

import org.springframework.stereotype.Service;

import com.schwab.urlshortener.exception.ShortCodeNotFoundException;
import com.schwab.urlshortener.model.UrlMapping;
import com.schwab.urlshortener.repository.UrlMappingRepository;

@Service
public class UrlAnalyticsService implements AnalyticsService {

    private final UrlMappingRepository urlMappingRepository;

    public UrlAnalyticsService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Override
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
}
