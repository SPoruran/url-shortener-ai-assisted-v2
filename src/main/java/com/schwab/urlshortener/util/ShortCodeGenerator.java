package com.schwab.urlshortener.util;

import java.security.SecureRandom;

public final class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;

    private ShortCodeGenerator() {
    }

    public static String generate() {
        return generate(new SecureRandom());
    }

    public static String generate(SecureRandom random) {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static boolean isValid(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            return false;
        }

        if (shortCode.length() != CODE_LENGTH) {
            return false;
        }

        for (char ch : shortCode.toCharArray()) {
            if (ALPHABET.indexOf(ch) < 0) {
                return false;
            }
        }

        return true;
    }
}
