package com.asifqureshi.gitforge.ui.theme;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import com.asifqureshi.gitforge.R;

/** Accent colour choices shown in Appearance settings (reference screen 16). */
public enum Accent {
    INDIGO("indigo", R.string.accent_indigo, R.style.ThemeOverlay_GitForge_Accent_Indigo,
            R.color.gf_accent_indigo, R.color.gf_on_accent_indigo),
    SLATE("slate", R.string.accent_slate, R.style.ThemeOverlay_GitForge_Accent_Slate,
            R.color.gf_accent_slate, R.color.gf_on_accent_slate),
    GREEN("green", R.string.accent_green, R.style.ThemeOverlay_GitForge_Accent_Green,
            R.color.gf_accent_green, R.color.gf_on_accent_green),
    CYAN("cyan", R.string.accent_cyan, R.style.ThemeOverlay_GitForge_Accent_Cyan,
            R.color.gf_accent_cyan, R.color.gf_on_accent_cyan),
    VIOLET("violet", R.string.accent_violet, R.style.ThemeOverlay_GitForge_Accent_Violet,
            R.color.gf_accent_violet, R.color.gf_on_accent_violet),
    PURPLE("purple", R.string.accent_purple, R.style.ThemeOverlay_GitForge_Accent_Purple,
            R.color.gf_accent_purple, R.color.gf_on_accent_purple),
    ORANGE("orange", R.string.accent_orange, R.style.ThemeOverlay_GitForge_Accent_Orange,
            R.color.gf_accent_orange, R.color.gf_on_accent_orange),
    AMBER("amber", R.string.accent_amber, R.style.ThemeOverlay_GitForge_Accent_Amber,
            R.color.gf_accent_amber, R.color.gf_on_accent_amber),
    RED("red", R.string.accent_red, R.style.ThemeOverlay_GitForge_Accent_Red,
            R.color.gf_accent_red, R.color.gf_on_accent_red);

    public static final Accent DEFAULT = INDIGO;

    private final String id;
    @StringRes
    private final int labelRes;
    @StyleRes
    private final int overlayRes;
    @ColorRes
    private final int colorRes;
    @ColorRes
    private final int onColorRes;

    Accent(String id, @StringRes int labelRes, @StyleRes int overlayRes,
           @ColorRes int colorRes, @ColorRes int onColorRes) {
        this.id = id;
        this.labelRes = labelRes;
        this.overlayRes = overlayRes;
        this.colorRes = colorRes;
        this.onColorRes = onColorRes;
    }

    @ColorRes
    public int getColorRes() {
        return colorRes;
    }

    @ColorRes
    public int getOnColorRes() {
        return onColorRes;
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
