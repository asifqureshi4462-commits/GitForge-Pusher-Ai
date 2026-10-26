package com.asifqureshi.gitforge.ui.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import com.asifqureshi.gitforge.R;

/** Accent colour choices shown in Appearance settings (reference screen 16). */
public enum Accent {
    INDIGO("indigo", R.string.accent_indigo, R.style.ThemeOverlay_GitForge_Accent_Indigo),
    SLATE("slate", R.string.accent_slate, R.style.ThemeOverlay_GitForge_Accent_Slate),
    GREEN("green", R.string.accent_green, R.style.ThemeOverlay_GitForge_Accent_Green),
    CYAN("cyan", R.string.accent_cyan, R.style.ThemeOverlay_GitForge_Accent_Cyan),
    VIOLET("violet", R.string.accent_violet, R.style.ThemeOverlay_GitForge_Accent_Violet),
    PURPLE("purple", R.string.accent_purple, R.style.ThemeOverlay_GitForge_Accent_Purple),
    ORANGE("orange", R.string.accent_orange, R.style.ThemeOverlay_GitForge_Accent_Orange),
    AMBER("amber", R.string.accent_amber, R.style.ThemeOverlay_GitForge_Accent_Amber),
    RED("red", R.string.accent_red, R.style.ThemeOverlay_GitForge_Accent_Red);

    public static final Accent DEFAULT = INDIGO;

    private final String id;
    @StringRes
    private final int labelRes;
    @StyleRes
    private final int overlayRes;

    Accent(String id, @StringRes int labelRes, @StyleRes int overlayRes) {
        this.id = id;
        this.labelRes = labelRes;
        this.overlayRes = overlayRes;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    @StyleRes
    public int getOverlayRes() {
        return overlayRes;
    }

    /** Resolves a stored id; unknown or missing ids fall back to {@link #DEFAULT}. */
    @NonNull
    public static Accent fromId(@Nullable String id) {
        if (id != null) {
            for (Accent accent : values()) {
                if (accent.id.equals(id)) {
                    return accent;
                }
            }
        }
        return DEFAULT;
    }
}
