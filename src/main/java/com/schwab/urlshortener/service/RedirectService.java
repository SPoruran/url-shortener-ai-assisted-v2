package com.schwab.urlshortener.service;

public interface RedirectService {
    String resolve(String shortCode);
}
