package com.asifqureshi.gitforge.domain.project;

/** How a staged file compares with the selected branch on GitHub. */
public enum FileStatus {
    /** Not yet compared with the remote branch. */
    UNKNOWN,
    NEW,
    MODIFIED,
    UNCHANGED
}
