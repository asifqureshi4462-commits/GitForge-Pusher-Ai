package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class PathSanitizerTest {

    @Test
    public void normalisesSeparatorsAndLeadingSlash() throws Exception {
        assertEquals("a/b/c.txt", PathSanitizer.sanitize("\\a\\b/./c.txt"));
        assertEquals("a/b", PathSanitizer.sanitize("///a//b/"));
    }

    @Test
    public void rejectsTraversal() {
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize("../etc/passwd"));
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize("a/../../b"));
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize("a\\..\\b"));
    }

    @Test
    public void rejectsDriveLettersControlCharsAndEmpty() {
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize("C:/Windows/x"));
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize("a\u0000b"));
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize("///"));
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize(null));
    }

    @Test
    public void rejectsOverlongPaths() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            sb.append('a');
        }
        assertThrows(PathSanitizer.UnsafePathException.class, () -> PathSanitizer.sanitize(sb.toString()));
    }

    @Test
    public void folderOrEmptyAllowsBlank() throws Exception {
        assertEquals("", PathSanitizer.sanitizeFolderOrEmpty("  "));
        assertEquals("docs/api", PathSanitizer.sanitizeFolderOrEmpty(" /docs/api/ "));
    }
}
