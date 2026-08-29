package com.schwab.urlshortener.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.schwab.urlshortener.model.UrlMapping;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByLongUrl(String longUrl);
}
