package com.asifqureshi.gitforge.domain;

import java.util.regex.Pattern;

/** Removes anything that looks like a credential from text before it is shown, stored or logged. */
public final class SecretRedactor {

    public static final int MAX_LENGTH = 500;
    private static final String MASK = "[redacted]";

    private static final Pattern[] TOKEN_PATTERNS = new Pattern[]{
            Pattern.compile("gh[pousr]_[A-Za-z0-9]{20,}"),
            Pattern.compile("github_pat_[A-Za-z0-9_]{20,}"),
            Pattern.compile("sk-[A-Za-z0-9_\\-]{16,}"),
            Pattern.compile("AIza[0-9A-Za-z_\\-]{30,}"),
            Pattern.compile("xox[baprs]-[0-9A-Za-z\\-]{10,}")
    };
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=\\-]{12,}");
    private static final Pattern KEYED = Pattern.compile(
            "(?i)((?:authorization|token|api[_-]?key|secret|password|x-api-key)[\"']?\\s*[:=]\\s*[\"']?)[A-Za-z0-9._~+/=\\-]{12,}");

    private SecretRedactor() {
    }

    public static String redact(String input) {
        if (input == null) {
            return "";
        }
        String s = input;
        for (Pattern p : TOKEN_PATTERNS) {
            s = p.matcher(s).replaceAll(MASK);
        }
        s = BEARER.matcher(s).replaceAll("Bearer " + MASK);
        s = KEYED.matcher(s).replaceAll("$1" + MASK);
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH) + "...";
        }
        return s;
    }
}
