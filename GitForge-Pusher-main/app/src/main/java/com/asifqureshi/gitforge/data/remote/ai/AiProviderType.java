package com.asifqureshi.gitforge.data.remote.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Supported provider protocols. "OpenAI-compatible" also covers any custom endpoint that speaks that API. */
public enum AiProviderType {
    OPENAI_COMPATIBLE("openai", "OpenAI-compatible", "https://api.openai.com/v1"),
    ANTHROPIC("anthropic", "Anthropic", "https://api.anthropic.com"),
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com");

    private final String id;
    private final String label;
    private final String defaultBaseUrl;

    AiProviderType(String id, String label, String defaultBaseUrl) {
        this.id = id;
        this.label = label;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    @Nullable
    public static AiProviderType fromId(@Nullable String id) {
        for (AiProviderType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }
}
