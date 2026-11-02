package com.asifqureshi.gitforge.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Non-sensitive user preferences. Secrets (GitHub tokens, AI keys) are NEVER stored here;
 * they live in {@link SecureStore} via {@link CredentialStore}.
 */
public final class SettingsStore {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final String AUTH_OAUTH = "oauth";
    public static final String AUTH_PAT = "pat";

    private static final String PREFS_NAME = "gitforge_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT_ID = "accent_id";
    private static final String KEY_ACCOUNT_LOGIN = "account_login";
    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String KEY_AUTH_METHOD = "auth_method";
    private static final String KEY_TOKEN_SCOPES = "token_scopes";
    private static final String KEY_AI_PROVIDER = "ai_provider";
    private static final String KEY_AI_BASE_URL = "ai_base_url";
    private static final String KEY_AI_MODEL = "ai_model";

    private final SharedPreferences prefs;

    public SettingsStore(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** One of {@link #THEME_SYSTEM}, {@link #THEME_LIGHT}, {@link #THEME_DARK}. */
    public int getThemeMode() {
        int mode = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
        if (mode < THEME_SYSTEM || mode > THEME_DARK) {
            return THEME_SYSTEM;
        }
        return mode;
    }

    public void setThemeMode(int mode) {
        if (mode < THEME_SYSTEM || mode > THEME_DARK) {
            throw new IllegalArgumentException("Unknown theme mode: " + mode);
        }
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /** Stored accent id, or null if the user never chose one (caller falls back to the default). */
    @Nullable
    public String getAccentId() {
        return prefs.getString(KEY_ACCENT_ID, null);
    }

    public void setAccentId(@NonNull String accentId) {
        prefs.edit().putString(KEY_ACCENT_ID, accentId).apply();
    }

    // ------------------------------------------------------------ account (non-secret)

    public void setAccount(@NonNull String login, @Nullable String name, @NonNull String method,
                           @Nullable String scopes) {
        prefs.edit().putString(KEY_ACCOUNT_LOGIN, login).putString(KEY_ACCOUNT_NAME, name)
                .putString(KEY_AUTH_METHOD, method).putString(KEY_TOKEN_SCOPES, scopes).apply();
    }

    public void clearAccount() {
        prefs.edit().remove(KEY_ACCOUNT_LOGIN).remove(KEY_ACCOUNT_NAME).remove(KEY_AUTH_METHOD)
                .remove(KEY_TOKEN_SCOPES).apply();
    }

    @Nullable
    public String getAccountLogin() {
        return prefs.getString(KEY_ACCOUNT_LOGIN, null);
    }

    @Nullable
    public String getAccountName() {
        return prefs.getString(KEY_ACCOUNT_NAME, null);
    }

    @Nullable
    public String getAuthMethod() {
        return prefs.getString(KEY_AUTH_METHOD, null);
    }

    @Nullable
    public String getTokenScopes() {
        return prefs.getString(KEY_TOKEN_SCOPES, null);
    }

    // ------------------------------------------------------------ AI provider (non-secret part)

    @Nullable
    public String getAiProvider() {
        return prefs.getString(KEY_AI_PROVIDER, null);
    }

    @Nullable
    public String getAiBaseUrl() {
        return prefs.getString(KEY_AI_BASE_URL, null);
    }

    @Nullable
    public String getAiModel() {
        return prefs.getString(KEY_AI_MODEL, null);
    }

    public void setAiConfig(@NonNull String provider, @Nullable String baseUrl, @NonNull String model) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider).putString(KEY_AI_BASE_URL, baseUrl)
                .putString(KEY_AI_MODEL, model).apply();
    }

    public void clearAiConfig() {
        prefs.edit().remove(KEY_AI_PROVIDER).remove(KEY_AI_BASE_URL).remove(KEY_AI_MODEL).apply();
    }
}
