package com.asifqureshi.gitforge.domain.github;

/** A GitHub/network failure with a message that is safe to show (never contains credentials). */
public class ApiException extends Exception {

    public enum Kind {
        NETWORK, UNAUTHORIZED, FORBIDDEN, RATE_LIMITED, NOT_FOUND, CONFLICT, EMPTY_REPOSITORY, VALIDATION, SERVER, UNKNOWN
    }

    private final Kind kind;
    private final int httpStatus;
    private final long retryAtEpochSeconds;

    public ApiException(Kind kind, int httpStatus, String safeMessage, long retryAtEpochSeconds) {
        super(safeMessage);
        this.kind = kind;
        this.httpStatus = httpStatus;
        this.retryAtEpochSeconds = retryAtEpochSeconds;
    }

    public ApiException(Kind kind, int httpStatus, String safeMessage) {
        this(kind, httpStatus, safeMessage, 0);
    }

    public static ApiException network() {
        return new ApiException(Kind.NETWORK, 0, "Network error. Check your connection and try again.");
    }

    public Kind getKind() {
        return kind;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** Epoch seconds at which a rate limit resets, or 0 when unknown. */
    public long getRetryAtEpochSeconds() {
        return retryAtEpochSeconds;
    }

    public boolean isAuthProblem() {
        return kind == Kind.UNAUTHORIZED;
    }
}
