package com.asifqureshi.gitforge.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Non-sensitive user preferences. Secrets (GitHub tokens, AI keys) are NEVER stored here;
 * they will live in a Keystore-backed store introduced in a later phase.
 */
public final class SettingsStore {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String PREFS_NAME = "gitforge_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT_ID = "accent_id";

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
}
