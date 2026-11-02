package com.asifqureshi.gitforge.data.model;

import androidx.annotation.Nullable;

/** The repository (and optionally branch) the user is working with. Held in memory only. */
public final class Selection {
    private final Repo repo;
    private final String branch;

    public Selection(Repo repo, @Nullable String branch) {
        this.repo = repo;
        this.branch = branch;
    }

    public Repo getRepo() {
        return repo;
    }

    @Nullable
    public String getBranch() {
        return branch;
    }

    public Selection withBranch(String newBranch) {
        return new Selection(repo, newBranch);
    }
}
