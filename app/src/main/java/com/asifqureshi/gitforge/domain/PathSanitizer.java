package com.asifqureshi.gitforge.domain;

import java.util.ArrayList;
import java.util.List;

/** Normalises and validates repository-relative paths coming from archives, folders and user input. */
public final class PathSanitizer {

    public static final int MAX_PATH_LENGTH = 1024;

    private PathSanitizer() {
    }

    public static final class UnsafePathException extends Exception {
        public UnsafePathException(String message) {
            super(message);
        }
    }

    /**
     * Returns a clean forward-slash relative path.
     * Rejects empty paths, ".." segments, drive letters, control characters and over-long paths.
     */
    public static String sanitize(String raw) throws UnsafePathException {
        if (raw == null) {
            throw new UnsafePathException("Path is missing");
        }
        String p = raw.replace('\\', '/');
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new UnsafePathException("Path contains control characters");
            }
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.length() >= 2 && p.charAt(1) == ':' && Character.isLetter(p.charAt(0))) {
            throw new UnsafePathException("Path contains a drive letter");
        }
        List<String> out = new ArrayList<>();
        for (String seg : p.split("/")) {
            if (seg.isEmpty() || seg.equals(".")) {
                continue;
            }
            if (seg.equals("..")) {
                throw new UnsafePathException("Path escapes the project folder");
            }
            out.add(seg);
        }
        if (out.isEmpty()) {
            throw new UnsafePathException("Path is empty");
        }
        String joined = String.join("/", out);
        if (joined.length() > MAX_PATH_LENGTH) {
            throw new UnsafePathException("Path is too long");
        }
        return joined;
    }

    /** Validates an optional target sub-folder. Empty input means the repository root. */
    public static String sanitizeFolderOrEmpty(String raw) throws UnsafePathException {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        return sanitize(raw.trim());
    }
}
