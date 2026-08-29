package com.schwab.urlshortener.exception;

public class DuplicateAliasException extends RuntimeException {

    public DuplicateAliasException(String alias) {
        super("Alias already exists: " + alias);
    }
}
