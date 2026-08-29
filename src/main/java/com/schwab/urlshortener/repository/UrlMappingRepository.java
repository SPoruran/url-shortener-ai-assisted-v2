package com.schwab.urlshortener.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.schwab.urlshortener.model.UrlMapping;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByLongUrl(String longUrl);

    @Modifying
    @Transactional
    @Query("update UrlMapping u set u.clickCount = u.clickCount + 1, u.lastAccessedAt = :lastAccessedAt where u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode, @Param("lastAccessedAt") Instant lastAccessedAt);
}
