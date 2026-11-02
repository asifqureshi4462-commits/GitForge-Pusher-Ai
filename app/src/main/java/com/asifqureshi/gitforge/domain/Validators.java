package com.asifqureshi.gitforge.domain;

/** Input validation. Every method returns null when the input is valid, otherwise a user-facing message. */
public final class Validators {

    public static final int MAX_COMMIT_MESSAGE = 10_000;

    private Validators() {
    }

    /** Trims surrounding whitespace that is commonly copied along with a token. */
    public static String normalizeToken(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return "Enter a token.";
        }
        if (token.length() < 20 || token.length() > 512) {
            return "This does not look like a GitHub token (unexpected length).";
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c <= 0x20 || c >= 0x7f) {
                return "A token contains only visible characters and no spaces. Check what you pasted.";
            }
        }
        return null;
    }

    public static String validateCommitMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Enter a commit message.";
        }
        if (message.length() > MAX_COMMIT_MESSAGE) {
            return "The commit message is too long.";
        }
        return null;
    }

    /** Subset of git check-ref-format that rejects every name Git would reject. */
    public static String validateBranchName(String name) {
        if (name == null || name.isEmpty()) {
            return "Enter a branch name.";
        }
        if (name.length() > 200) {
            return "The branch name is too long.";
        }
        if (name.equals("@") || name.startsWith("/") || name.endsWith("/") || name.endsWith(".")
                || name.startsWith("-") || name.contains("//") || name.contains("..") || name.contains("@{")) {
            return "This is not a valid branch name.";
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c <= 0x20 || c == 0x7f || "~^:?*[\\".indexOf(c) >= 0) {
                return "A branch name cannot contain spaces or any of ~ ^ : ? * [ \\";
            }
        }
        for (String part : name.split("/")) {
            if (part.startsWith(".") || part.endsWith(".lock")) {
                return "Branch name parts cannot start with '.' or end with '.lock'.";
            }
        }
        return null;
    }
}
