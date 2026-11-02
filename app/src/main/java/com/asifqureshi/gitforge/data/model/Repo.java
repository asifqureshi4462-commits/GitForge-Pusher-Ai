package com.asifqureshi.gitforge.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Repo {
    private final String owner;
    private final String name;
    private final boolean isPrivate;
    private final String description;
    private final String defaultBranch;
    private final String htmlUrl;
    private final boolean canPush;
    private final boolean archived;

    public Repo(String owner, String name, boolean isPrivate, @Nullable String description,
                @Nullable String defaultBranch, String htmlUrl, boolean canPush, boolean archived) {
        this.owner = owner;
        this.name = name;
        this.isPrivate = isPrivate;
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.htmlUrl = htmlUrl;
        this.canPush = canPush;
        this.archived = archived;
    }

    @NonNull
    public String getOwner() {
        return owner;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getFullName() {
        return owner + "/" + name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getDefaultBranch() {
        return defaultBranch;
    }

    @NonNull
    public String getHtmlUrl() {
        return htmlUrl;
    }

    /** True when GitHub reports that the authenticated user may push to this repository. */
    public boolean canPush() {
        return canPush;
    }

    public boolean isArchived() {
        return archived;
    }
}
