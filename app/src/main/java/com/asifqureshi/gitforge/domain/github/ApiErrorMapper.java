package com.asifqureshi.gitforge.domain.github;

import com.asifqureshi.gitforge.domain.SecretRedactor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts HTTP failures into {@link ApiException}s with safe, actionable messages. */
public final class ApiErrorMapper {

    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private ApiErrorMapper() {
    }

    public static ApiException map(int status, String body, String rateLimitRemaining,
                                   String rateLimitReset, String retryAfter) {
        String ghMessage = extractMessage(body);
        String lower = ghMessage.toLowerCase(Locale.ROOT);

        switch (status) {
            case 401:
                return new ApiException(ApiException.Kind.UNAUTHORIZED, status,
                        "GitHub rejected the credentials. The token may be invalid, expired or revoked. Sign in again.");
            case 403:
            case 429: {
                boolean limited = retryAfter != null || "0".equals(rateLimitRemaining)
                        || lower.contains("rate limit") || status == 429;
                if (limited) {
                    long resetAt = 0;
                    if (retryAfter != null) {
                        resetAt = System.currentTimeMillis() / 1000 + parseLong(retryAfter);
                    } else {
                        resetAt = parseLong(rateLimitReset);
                    }
                    return new ApiException(ApiException.Kind.RATE_LIMITED, status,
                            "GitHub rate limit reached.", resetAt);
                }
                return new ApiException(ApiException.Kind.FORBIDDEN, status,
                        "The token does not have permission for this action. " + suffix(ghMessage));
            }
            case 404:
                return new ApiException(ApiException.Kind.NOT_FOUND, status,
                        "Not found. The repository or branch may not exist, or the token cannot access it "
                                + "(private repositories need repository access or the 'repo' scope).");
            case 409:
                if (lower.contains("repository is empty")) {
                    return new ApiException(ApiException.Kind.EMPTY_REPOSITORY, status,
                            "This repository is empty. Create an initial commit on GitHub first "
                                    + "(for example add a README), then try again.");
                }
                return new ApiException(ApiException.Kind.CONFLICT, status,
                        "GitHub reported a conflict. " + suffix(ghMessage));
            case 422:
                if (lower.contains("not a fast forward") || lower.contains("not a fast-forward")) {
                    return new ApiException(ApiException.Kind.CONFLICT, status,
                            "The branch was updated on GitHub while uploading. Nothing was overwritten. "
                                    + "Run the preview again to compare with the latest content.");
                }
                return new ApiException(ApiException.Kind.VALIDATION, status,
                        "GitHub rejected the request. " + suffix(ghMessage));
            default:
                if (status >= 500) {
                    return new ApiException(ApiException.Kind.SERVER, status,
                            "GitHub is having problems (HTTP " + status + "). Try again later.");
                }
                return new ApiException(ApiException.Kind.UNKNOWN, status,
                        "Unexpected response from GitHub (HTTP " + status + "). " + suffix(ghMessage));
        }
    }

    static String extractMessage(String body) {
        if (body == null) {
            return "";
        }
        Matcher m = MESSAGE.matcher(body);
        if (!m.find()) {
            return "";
        }
        String raw = m.group(1).replace("\\\"", "\"").replace("\\n", " ").replace("\\\\", "\\");
        return SecretRedactor.redact(raw);
    }

    private static String suffix(String ghMessage) {
        return ghMessage.isEmpty() ? "" : "GitHub says: " + ghMessage;
    }

    private static long parseLong(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
