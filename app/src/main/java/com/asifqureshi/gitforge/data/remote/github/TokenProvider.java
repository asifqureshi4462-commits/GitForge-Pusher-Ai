package com.asifqureshi.gitforge.data.remote.github;

import androidx.annotation.Nullable;

public interface TokenProvider {
    /** Current GitHub access token, or null when signed out. */
    @Nullable
    String getToken();
}
