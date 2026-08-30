package com.schwab.urlshortener.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AliasValidator {

    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,32}$");
    private static final Set<String> RESERVED_ALIASES = Set.of("api", "health", "shorten", "stats");

    public String validateAndNormalize(String customAlias) {
        String normalizedAlias = customAlias.trim();

        if (!CUSTOM_ALIAS_PATTERN.matcher(normalizedAlias).matches()) {
            throw new IllegalArgumentException("customAlias must be 4-32 characters long and contain only letters or numbers");
        }

        String lowerCaseAlias = normalizedAlias.toLowerCase(Locale.ROOT);
        if (RESERVED_ALIASES.contains(lowerCaseAlias)) {
            throw new IllegalArgumentException("customAlias is reserved and cannot be used");
        }

        return normalizedAlias;
    }
}
