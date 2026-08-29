package com.schwab.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/shorten.
 */
public class ShortenRequest {

    @NotBlank(message = "longUrl must not be blank")
    @Pattern(
            regexp = "^(https?)://[^\\s/$.?#].[^\\s]*$",
            message = "longUrl must be a valid http/https URL"
    )
    private String longUrl;

    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "customAlias must contain only letters and numbers")
    @Size(min = 4, max = 32, message = "customAlias must be between 4 and 32 characters")
    private String customAlias;

    @Positive(message = "expiresInSeconds must be greater than 0 when provided")
    private Long expiresInSeconds;

    public ShortenRequest() {
    }

    public ShortenRequest(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
