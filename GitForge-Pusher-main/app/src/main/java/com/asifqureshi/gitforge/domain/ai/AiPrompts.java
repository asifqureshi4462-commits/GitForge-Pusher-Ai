package com.asifqureshi.gitforge.domain.ai;

import com.asifqureshi.gitforge.domain.SecretScanner;

import java.util.List;

/** Builds prompts for the three AI features and guards what may be sent to a provider. */
public final class AiPrompts {

    public static final int MAX_INPUT_CHARS = 24_000;

    public enum Task { README, REVIEW, EXPLAIN_ERROR }

    private AiPrompts() {
    }

    public static String systemPrompt(Task task) {
        switch (task) {
            case README:
                return "You write clear, accurate README.md files in Markdown. Use only facts present in the supplied "
                        + "file list and snippets. If something is unknown, leave it out rather than guessing.";
            case REVIEW:
                return "You are a careful code reviewer. List concrete problems (bugs, security issues, unclear code) "
                        + "with the file name and why it matters, then suggest fixes. Do not invent code that was not shown.";
            default:
                return "You explain error messages and stack traces to developers. State the likely cause, how to "
                        + "confirm it, and how to fix it. Say so when the supplied text is not enough to be sure.";
        }
    }

    public static String userPrompt(Task task, String content) {
        String body = truncate(content);
        switch (task) {
            case README:
                return "Write a README.md for this project.\n\n" + body;
            case REVIEW:
                return "Review the following code.\n\n" + body;
            default:
                return "Explain this error and how to fix it.\n\n" + body;
        }
    }

    public static String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= MAX_INPUT_CHARS ? content : content.substring(0, MAX_INPUT_CHARS);
    }

    /** Labels of credential-like content in the text (never the values). Empty means nothing was detected. */
    public static List<String> findSecrets(String content) {
        return SecretScanner.scanText(truncate(content));
    }
}
