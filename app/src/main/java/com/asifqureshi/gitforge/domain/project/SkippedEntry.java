package com.asifqureshi.gitforge.domain.project;

public final class SkippedEntry {
    private final String path;
    private final SkipReason reason;

    public SkippedEntry(String path, SkipReason reason) {
        this.path = path;
        this.reason = reason;
    }

    public String getPath() {
        return path;
    }

    public SkipReason getReason() {
        return reason;
    }
}
