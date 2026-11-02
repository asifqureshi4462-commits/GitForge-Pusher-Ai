package com.asifqureshi.gitforge.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Flags files that probably contain credentials. Findings are short labels only - matched values are never
 * returned, logged or stored. Patterns are immutable and thread-safe (no stateful /g matching).
 */
public final class SecretScanner {

    public static final int MAX_SCAN_BYTES = 1024 * 1024;

    private static final class Rule {
        final String label;
        final Pattern pattern;

        Rule(String label, String regex) {
            this.label = label;
            this.pattern = Pattern.compile(regex);
        }
    }

    private static final Rule[] CONTENT_RULES = new Rule[]{
            new Rule("AWS access key", "AKIA[0-9A-Z]{16}"),
            new Rule("GitHub token", "gh[pousr]_[A-Za-z0-9]{30,}"),
            new Rule("GitHub fine-grained token", "github_pat_[A-Za-z0-9_]{30,}"),
            new Rule("API secret key (sk-...)", "sk-[A-Za-z0-9_\\-]{20,}"),
            new Rule("Stripe live key", "sk_live_[0-9a-zA-Z]{20,}"),
            new Rule("Google API key", "AIza[0-9A-Za-z_\\-]{35}"),
            new Rule("Slack token", "xox[baprs]-[0-9A-Za-z\\-]{10,}"),
            new Rule("Private key block", "-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY"),
            new Rule("Hard-coded credential assignment",
                    "(?i)(?:password|passwd|secret|api[_-]?key|access[_-]?token)\\s*[:=]\\s*['\"][^'\"\\s]{8,}['\"]")
    };

    private SecretScanner() {
    }

    /** Scans the file name and (for text files) the first {@value #MAX_SCAN_BYTES} bytes. */
    public static List<String> scan(String relPath, File file) throws IOException {
        List<String> findings = new ArrayList<>();
        String nameFinding = scanName(relPath);
        if (nameFinding != null) {
            findings.add(nameFinding);
        }
        byte[] head = readHead(file);
        if (!looksBinary(head)) {
            String text = new String(head, StandardCharsets.ISO_8859_1);
            for (Rule rule : CONTENT_RULES) {
                if (rule.pattern.matcher(text).find() && !findings.contains(rule.label)) {
                    findings.add(rule.label);
                }
            }
        }
        return findings;
    }

    /** Content-only scan for text that is about to be sent somewhere else (e.g. an AI provider). */
    public static List<String> scanText(String text) {
        List<String> findings = new ArrayList<>();
        for (Rule rule : CONTENT_RULES) {
            if (rule.pattern.matcher(text).find()) {
                findings.add(rule.label);
            }
        }
        return findings;
    }

    static String scanName(String relPath) {
        String name = relPath.substring(relPath.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        if (name.equals(".env") || (name.startsWith(".env.") && !name.endsWith(".example")
                && !name.endsWith(".sample") && !name.endsWith(".template"))) {
            return "Environment file";
        }
        if (name.equals("id_rsa") || name.equals("id_dsa") || name.equals("id_ecdsa") || name.equals("id_ed25519")) {
            return "SSH private key file";
        }
        if (name.endsWith(".pem") || name.endsWith(".pfx") || name.endsWith(".p12")
                || name.endsWith(".jks") || name.endsWith(".keystore") || name.endsWith(".ppk")) {
            return "Key or certificate file";
        }
        if (name.equals("credentials.json") || name.startsWith("service-account") && name.endsWith(".json")
                || name.equals("wp-config.php") || name.equals(".npmrc") || name.equals(".pypirc")
                || name.equals(".netrc")) {
            return "Credentials file";
        }
        return null;
    }

    private static byte[] readHead(File file) throws IOException {
        long len = Math.min(file.length(), MAX_SCAN_BYTES);
        byte[] buf = new byte[(int) len];
        int off = 0;
        try (InputStream in = new FileInputStream(file)) {
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) {
                    break;
                }
                off += n;
            }
        }
        if (off < buf.length) {
            byte[] trimmed = new byte[off];
            System.arraycopy(buf, 0, trimmed, 0, off);
            return trimmed;
        }
        return buf;
    }

    private static boolean looksBinary(byte[] data) {
        int limit = Math.min(data.length, 8000);
        for (int i = 0; i < limit; i++) {
            if (data[i] == 0) {
                return true;
            }
        }
        return false;
    }
}
