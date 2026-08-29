package com.schwab.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
}
