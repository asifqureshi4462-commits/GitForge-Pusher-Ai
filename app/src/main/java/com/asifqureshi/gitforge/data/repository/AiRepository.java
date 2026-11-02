package com.asifqureshi.gitforge.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.asifqureshi.gitforge.data.local.CredentialStore;
import com.asifqureshi.gitforge.data.local.SecureStore;
import com.asifqureshi.gitforge.data.local.SettingsStore;
import com.asifqureshi.gitforge.data.remote.ai.AiClient;
import com.asifqureshi.gitforge.data.remote.ai.AiException;
import com.asifqureshi.gitforge.data.remote.ai.AiProviderType;
import com.asifqureshi.gitforge.domain.ai.AiPrompts;
import com.asifqureshi.gitforge.domain.ai.AiValidators;

/** Provider configuration (non-secret part in settings, key encrypted) and AI calls. Methods block. */
public final class AiRepository {

    private final SettingsStore settings;
    private final CredentialStore credentials;
    private final AiClient client;

    public AiRepository(@NonNull SettingsStore settings, @NonNull CredentialStore credentials,
                        @NonNull AiClient client) {
        this.settings = settings;
        this.credentials = credentials;
        this.client = client;
    }

    @Nullable
    public AiProviderType getProvider() {
        return AiProviderType.fromId(settings.getAiProvider());
    }

    @Nullable
    public String getBaseUrl() {
        return settings.getAiBaseUrl();
    }

    @Nullable
    public String getModel() {
        return settings.getAiModel();
    }

    /** True only when provider, endpoint, model and a stored key are all present. */
    public boolean isConfigured() {
        AiProviderType type = getProvider();
        return type != null && settings.getAiBaseUrl() != null && settings.getAiModel() != null
                && credentials.getAiKey(type.getId()) != null;
    }

    public boolean hasKey(@NonNull AiProviderType type) {
        return credentials.getAiKey(type.getId()) != null;
    }

    /** Validates and stores the configuration. A blank key keeps an already stored key for this provider. */
    public void saveConfig(@NonNull AiProviderType type, @NonNull String baseUrl, @NonNull String model,
                           @Nullable String apiKey) throws IllegalArgumentException, SecureStore.SecureStoreException {
        String urlProblem = AiValidators.validateBaseUrl(baseUrl);
        if (urlProblem != null) {
            throw new IllegalArgumentException(urlProblem);
        }
        String modelProblem = AiValidators.validateModel(model);
        if (modelProblem != null) {
            throw new IllegalArgumentException(modelProblem);
        }
        boolean newKey = apiKey != null && !apiKey.trim().isEmpty();
        if (newKey) {
            String keyProblem = AiValidators.validateApiKey(apiKey);
            if (keyProblem != null) {
                throw new IllegalArgumentException(keyProblem);
            }
        } else if (!hasKey(type)) {
            throw new IllegalArgumentException("Enter an API key.");
        }
        if (newKey) {
            credentials.saveAiKey(type.getId(), apiKey.trim());
        }
        settings.setAiConfig(type.getId(), AiValidators.normalizeBaseUrl(baseUrl), model.trim());
    }

    public void removeConfig() {
        AiProviderType type = getProvider();
        if (type != null) {
            credentials.clearAiKey(type.getId());
        }
        settings.clearAiConfig();
    }

    @NonNull
    public String run(@NonNull AiPrompts.Task task, @NonNull String content) throws AiException {
        AiProviderType type = getProvider();
        String baseUrl = settings.getAiBaseUrl();
        String model = settings.getAiModel();
        String key = type != null ? credentials.getAiKey(type.getId()) : null;
        if (type == null || baseUrl == null || model == null || key == null) {
            throw new AiException("AI is not configured. Add a provider, model and API key in AI settings.");
        }
        return client.complete(type, baseUrl, model, key, AiPrompts.systemPrompt(task),
                AiPrompts.userPrompt(task, content));
    }
}
