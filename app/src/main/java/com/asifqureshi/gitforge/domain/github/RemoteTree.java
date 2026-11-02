package com.asifqureshi.gitforge.domain.github;

import java.util.HashMap;
import java.util.Map;

/** Blob entries of a remote tree (path to blob sha and file mode). */
public final class RemoteTree {

    private final Map<String, String> shas = new HashMap<>();
    private final Map<String, String> modes = new HashMap<>();
    private final boolean truncated;

    public RemoteTree(boolean truncated) {
        this.truncated = truncated;
    }

    public void putBlob(String path, String sha, String mode) {
        shas.put(path, sha);
        modes.put(path, mode);
    }

    public String getBlobSha(String path) {
        return shas.get(path);
    }

    public String getMode(String path) {
        return modes.get(path);
    }

    /** True when GitHub cut the listing short, so a missing path is not proof that it does not exist. */
    public boolean isTruncated() {
        return truncated;
    }

    public int size() {
        return shas.size();
    }
}
