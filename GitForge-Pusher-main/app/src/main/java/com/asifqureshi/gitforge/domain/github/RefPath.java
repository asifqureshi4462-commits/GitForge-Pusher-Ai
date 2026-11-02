package com.asifqureshi.gitforge.domain.github;

import java.nio.charset.StandardCharsets;

/** Encodes branch names for use inside a URL path while keeping the slashes of names like feature/login. */
public final class RefPath {

    private RefPath() {
    }

    public static String encode(String branch) {
        byte[] bytes = branch.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length + 8);
        for (byte b : bytes) {
            int c = b & 0xff;
            boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~' || c == '/';
            if (unreserved) {
                sb.append((char) c);
            } else {
                sb.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return sb.toString();
    }
}
