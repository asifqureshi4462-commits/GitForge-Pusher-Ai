package com.asifqureshi.gitforge.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Computes Git blob object ids: SHA-1 of "blob &lt;size&gt;\0" followed by the content. */
public final class GitBlobHasher {

    private GitBlobHasher() {
    }

    public static String hash(byte[] data) {
        MessageDigest md = newDigest();
        md.update(header(data.length));
        md.update(data);
        return hex(md.digest());
    }

    public static String hash(File file) throws IOException {
        long length = file.length();
        MessageDigest md = newDigest();
        md.update(header(length));
        long read = 0;
        byte[] buf = new byte[64 * 1024];
        try (InputStream in = new FileInputStream(file)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
                read += n;
            }
        }
        if (read != length) {
            throw new IOException("File changed while hashing");
        }
        return hex(md.digest());
    }

    private static byte[] header(long length) {
        return ("blob " + length + "\0").getBytes(StandardCharsets.US_ASCII);
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }
}
