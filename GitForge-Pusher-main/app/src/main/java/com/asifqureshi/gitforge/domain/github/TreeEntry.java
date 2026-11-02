package com.asifqureshi.gitforge.domain.github;

public final class TreeEntry {
    private final String path;
    private final String mode;
    private final String sha;

    public TreeEntry(String path, String mode, String sha) {
        this.path = path;
        this.mode = mode;
        this.sha = sha;
    }

    public String getPath() {
        return path;
    }

    public String getMode() {
        return mode;
    }

    public String getSha() {
        return sha;
    }
}
