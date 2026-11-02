package com.asifqureshi.gitforge.domain.github;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RefPathTest {

    @Test
    public void keepsSlashesAndSafeCharacters() {
        assertEquals("feature/login-v1.2_x~y", RefPath.encode("feature/login-v1.2_x~y"));
    }

    @Test
    public void encodesUnsafeCharacters() {
        assertEquals("release%20candidate/%23123", RefPath.encode("release candidate/#123"));
        assertEquals("caf%C3%A9", RefPath.encode("caf\u00e9"));
        assertEquals("a%25b%3Fc", RefPath.encode("a%b?c"));
    }
}
