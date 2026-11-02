package com.asifqureshi.gitforge.data.model;

import androidx.annotation.Nullable;

public final class UserProfile {
    private final String login;
    private final String name;
    private final String avatarUrl;
    private final int publicRepos;
    private final Integer totalPrivateRepos;

    public UserProfile(String login, @Nullable String name, @Nullable String avatarUrl,
                       int publicRepos, @Nullable Integer totalPrivateRepos) {
        this.login = login;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.publicRepos = publicRepos;
        this.totalPrivateRepos = totalPrivateRepos;
    }

    public String getLogin() {
        return login;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() ? name : login;
    }

    /** Public repositories plus private ones when GitHub reported them for this token. */
    public int getRepoCount() {
        return publicRepos + (totalPrivateRepos != null ? totalPrivateRepos : 0);
    }

    public boolean includesPrivateCount() {
        return totalPrivateRepos != null;
    }
}
