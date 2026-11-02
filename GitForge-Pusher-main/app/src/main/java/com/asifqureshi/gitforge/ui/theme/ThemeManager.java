package com.asifqureshi.gitforge.ui.theme;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.asifqureshi.gitforge.data.local.SettingsStore;

/** Applies the user's theme mode and accent colour. */
public final class ThemeManager {

    private ThemeManager() {
    }

    /** Maps a {@link SettingsStore} theme mode to AppCompat's night mode and applies it app-wide. */
    public static void applyNightMode(int settingsThemeMode) {
        int nightMode;
        switch (settingsThemeMode) {
            case SettingsStore.THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case SettingsStore.THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case SettingsStore.THEME_SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /** Layers the chosen accent overlay on the activity theme. Call before inflating views. */
    public static void applyAccent(@NonNull Activity activity, @NonNull SettingsStore settings) {
        Accent accent = Accent.fromId(settings.getAccentId());
        activity.getTheme().applyStyle(accent.getOverlayRes(), true);
    }
}
