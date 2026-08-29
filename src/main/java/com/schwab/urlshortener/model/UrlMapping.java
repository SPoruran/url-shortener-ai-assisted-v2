package com.schwab.urlshortener.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "url_mapping",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_url_mapping_short_code", columnNames = "short_code"),
        @UniqueConstraint(name = "uk_url_mapping_long_url", columnNames = "long_url")
    }
)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 64)
    private String shortCode;

    @Column(name = "long_url", nullable = false, unique = true, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UrlMapping() {
        // JPA
    }

    public UrlMapping(String shortCode, String longUrl, Instant createdAt) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
