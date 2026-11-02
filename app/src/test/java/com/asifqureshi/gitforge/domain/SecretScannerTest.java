package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

public class SecretScannerTest {

    private static List<String> scan(String name, String content) throws Exception {
        File f = File.createTempFile("scan", ".tmp");
        try {
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return SecretScanner.scan(name, f);
        } finally {
            f.delete();
        }
    }

    @Test
    public void flagsSensitiveFileNames() throws Exception {
        assertFalse(scan(".env", "A=1").isEmpty());
        assertFalse(scan("config/.env.production", "A=1").isEmpty());
        assertFalse(scan("keys/id_rsa", "x").isEmpty());
        assertFalse(scan("server.pem", "x").isEmpty());
        assertTrue(scan(".env.example", "A=1").isEmpty());
        assertTrue(scan("README.md", "hello").isEmpty());
    }

    @Test
    public void flagsKnownTokenShapesWithoutReturningTheValue() throws Exception {
        String ghToken = "ghp_" + "a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8";
        List<String> findings = scan("notes.txt", "token = " + ghToken);
        assertTrue(findings.contains("GitHub token"));
        for (String f : findings) {
            assertFalse(f.contains(ghToken));
        }
        assertTrue(scan("a.txt", "-----BEGIN RSA PRIVATE KEY-----").contains("Private key block"));
        assertTrue(scan("a.txt", "AKIAABCDEFGHIJKLMNOP").contains("AWS access key"));
    }

    @Test
    public void googleKeyPatternMatchesHyphenAndUnderscore() throws Exception {
        String key = "AIza" + "SyA-1234567890_abcdefghijklmnopqrst";
        assertEquals(35, key.length() - 4);
        assertTrue(scan("a.js", "const k='" + key + "'").contains("Google API key"));
    }

    @Test
    public void repeatedScansAreStable() throws Exception {
        // Regression for the shared stateful /g regex bug in the old web app.
        String content = "AKIAABCDEFGHIJKLMNOP";
        for (int i = 0; i < 5; i++) {
            assertTrue(scan("a.txt", content).contains("AWS access key"));
        }
    }

    @Test
    public void skipsBinaryContent() throws Exception {
        assertTrue(scan("image.bin", "AKIAABCDEFGHIJKLMNOP\u0000binary").isEmpty());
    }

    @Test
    public void scanTextFindsSecretsInPastedText() {
        assertFalse(SecretScanner.scanText("password = 'hunter2hunter2'").isEmpty());
        assertTrue(SecretScanner.scanText("just some code").isEmpty());
    }
}
