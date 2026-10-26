package com.asifqureshi.gitforge;

import android.content.Context;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.data.local.SettingsStore;

/**
 * Manual dependency container (no DI framework; see docs/ARCHITECTURE.md).
 * Later phases add the Room database, SecureStore, Retrofit clients and repositories here.
 */
public final class AppContainer {

    private final SettingsStore settings;

    AppContainer(@NonNull Context applicationContext) {
        this.settings = new SettingsStore(applicationContext);
    }

    @NonNull
    public SettingsStore getSettings() {
        return settings;
    }
}
