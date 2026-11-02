package com.asifqureshi.gitforge.domain.project;

public enum SkipReason {
    IGNORED("Ignored (VCS, OS or dependency folder)"),
    TOO_LARGE("Larger than the per-file limit"),
    UNSAFE_PATH("Unsafe path"),
    DUPLICATE("Duplicate path"),
    UNREADABLE("Could not be read");

    private final String description;

    SkipReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
