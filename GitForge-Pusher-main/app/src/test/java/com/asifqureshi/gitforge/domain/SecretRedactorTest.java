package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SecretRedactorTest {

    @Test
    public void masksTokensAndBearerHeaders() {
        String token = "ghp_" + "a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8";
        String out = SecretRedactor.redact("failed with Authorization: Bearer " + token + " and " + token);
        assertFalse(out.contains(token));
        assertTrue(out.contains("[redacted]"));
    }

    @Test
    public void masksKeyedValues() {
        String out = SecretRedactor.redact("api_key=abcdEFGH12345678xyz");
        assertFalse(out.contains("abcdEFGH12345678xyz"));
        assertTrue(out.startsWith("api_key="));
    }

    @Test
    public void truncatesAndHandlesNull() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 900; i++) {
            sb.append('x');
        }
        assertEquals(SecretRedactor.MAX_LENGTH + 3, SecretRedactor.redact(sb.toString()).length());
        assertEquals("", SecretRedactor.redact(null));
    }
}
