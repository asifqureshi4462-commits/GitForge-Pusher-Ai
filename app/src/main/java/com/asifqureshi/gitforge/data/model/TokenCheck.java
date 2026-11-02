package com.asifqureshi.gitforge.data.model;

import androidx.annotation.Nullable;

/** Result of validating a token against GitHub: who it belongs to and (classic tokens) its scopes. */
public final class TokenCheck {
    private final UserProfile profile;
    private final String scopes;

    public TokenCheck(UserProfile profile, @Nullable String scopes) {
        this.profile = profile;
        this.scopes = scopes;
    }

    public UserProfile getProfile() {
        return profile;
    }

    /** Value of the X-OAuth-Scopes header, or null for fine-grained tokens that do not report scopes. */
    @Nullable
    public String getScopes() {
        return scopes;
    }
}
