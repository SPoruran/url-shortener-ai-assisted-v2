package com.schwab.urlshortener;

import static org.junit.jupiter.api.Assertions.*;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import com.schwab.urlshortener.util.ShortCodeGenerator;

class ShortCodeGeneratorTest {

    @Test
    void generate_returnsSevenCharacterCode() {
        String code = ShortCodeGenerator.generate();

        assertEquals(7, code.length());
        assertTrue(ShortCodeGenerator.isValid(code));
    }

    @Test
    void generate_usesSecureRandomAndCreatesAllowedCharacters() {
        String code = ShortCodeGenerator.generate(new SecureRandom());

        assertEquals(7, code.length());
        assertTrue(ShortCodeGenerator.isValid(code));
        assertFalse(code.chars().anyMatch(ch -> ch == ' ' || ch == '/'));
    }

    @Test
    void isValid_rejectsNullBlankAndWrongLength() {
        assertFalse(ShortCodeGenerator.isValid(null));
        assertFalse(ShortCodeGenerator.isValid(""));
        assertFalse(ShortCodeGenerator.isValid("ABC"));
    }

    @Test
    void isValid_rejectsDisallowedCharacters() {
        assertFalse(ShortCodeGenerator.isValid("bad_alias"));
        assertFalse(ShortCodeGenerator.isValid("abc/def"));
    }
}
