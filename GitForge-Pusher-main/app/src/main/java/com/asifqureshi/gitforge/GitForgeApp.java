package com.asifqureshi.gitforge;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.ui.theme.ThemeManager;

/**
 * Application entry point. Owns the {@link AppContainer} and applies the saved
 * System/Light/Dark mode before any activity is created.
 */
public class GitForgeApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        ThemeManager.applyNightMode(container.getSettings().getThemeMode());
    }

    @NonNull
    public AppContainer getContainer() {
        return container;
    }

    /** Convenience accessor usable from any Context. */
    @NonNull
    public static AppContainer container(@NonNull Context context) {
        return ((GitForgeApp) context.getApplicationContext()).getContainer();
    }
}
