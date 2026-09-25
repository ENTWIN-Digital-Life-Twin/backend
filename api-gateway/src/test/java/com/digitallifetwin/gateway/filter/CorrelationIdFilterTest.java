package com.digitallifetwin.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CorrelationIdFilterTest {

    @Test
    void generatesWhenNull() {
        String id = CorrelationIdFilter.sanitizeOrGenerate(null);
        assertTrue(id.length() >= 32);
    }

    @Test
    void preservesValid() {
        assertEquals("abc-123", CorrelationIdFilter.sanitizeOrGenerate("abc-123"));
    }

    @Test
    void replacesInvalidCharacters() {
        String id = CorrelationIdFilter.sanitizeOrGenerate("bad id with spaces!");
        assertNotEquals("bad id with spaces!", id);
    }

    @Test
    void replacesOversized() {
        String oversized = "a".repeat(100);
        assertNotEquals(oversized, CorrelationIdFilter.sanitizeOrGenerate(oversized));
    }

    @Test
    void preservesValidWithHyphen() {
        assertEquals("corr-test-12345", CorrelationIdFilter.sanitizeOrGenerate("corr-test-12345"));
    }
}
