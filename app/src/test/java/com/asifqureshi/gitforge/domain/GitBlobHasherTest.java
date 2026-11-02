package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class GitBlobHasherTest {

    // Values produced by `git hash-object`.
    @Test
    public void emptyBlob() {
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", GitBlobHasher.hash(new byte[0]));
    }

    @Test
    public void helloNewline() {
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a",
                GitBlobHasher.hash("hello\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void fileAndBytesAgree() throws Exception {
        File f = File.createTempFile("blob", ".txt");
        try {
            Files.write(f.toPath(), "hello\n".getBytes(StandardCharsets.UTF_8));
            assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", GitBlobHasher.hash(f));
        } finally {
            f.delete();
        }
    }
}
