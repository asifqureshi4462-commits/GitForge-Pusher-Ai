package com.asifqureshi.gitforge.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.asifqureshi.gitforge.data.remote.github.TokenProvider;

/** Typed access to the secrets kept in {@link SecureStore}. Secrets are never stored anywhere else. */
public final class CredentialStore implements TokenProvider {

    private static final String KEY_GITHUB_TOKEN = "github_token";
    private static final String KEY_AI_PREFIX = "ai_key_";

    private final SecureStore secure;
    @Nullable
    private volatile String cachedToken;

    public CredentialStore(@NonNull SecureStore secure) {
        this.secure = secure;
    }

    @Nullable
    @Override
    public String getToken() {
        String t = cachedToken;
        if (t == null) {
            t = secure.getString(KEY_GITHUB_TOKEN);
            cachedToken = t;
        }
        return t;
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    public void saveToken(@NonNull String token) throws SecureStore.SecureStoreException {
        secure.putString(KEY_GITHUB_TOKEN, token);
        cachedToken = token;
    }

    public void clearToken() {
        cachedToken = null;
        secure.remove(KEY_GITHUB_TOKEN);
    }

    @Nullable
    public String getAiKey(@NonNull String providerId) {
        return secure.getString(KEY_AI_PREFIX + providerId);
    }

    public void saveAiKey(@NonNull String providerId, @NonNull String key) throws SecureStore.SecureStoreException {
        secure.putString(KEY_AI_PREFIX + providerId, key);
    }

    public void clearAiKey(@NonNull String providerId) {
        secure.remove(KEY_AI_PREFIX + providerId);
    }
}
