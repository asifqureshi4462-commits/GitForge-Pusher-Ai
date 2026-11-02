package com.asifqureshi.gitforge.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.data.local.SecureStore;
import com.asifqureshi.gitforge.data.remote.ai.AiProviderType;
import com.asifqureshi.gitforge.data.repository.AiRepository;

public class AiSettingsViewModel extends AndroidViewModel {

    /** Outcome of a save: {@code error == null} means saved. */
    public static final class SaveResult {
        @Nullable
        public final String error;

        SaveResult(@Nullable String error) {
            this.error = error;
        }
    }

    private final AiRepository ai;
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();

    public AiSettingsViewModel(@NonNull Application application) {
        super(application);
        ai = GitForgeApp.container(application).getAi();
    }

    public LiveData<SaveResult> getSaveResult() {
        return saveResult;
    }

    public void consumeSaveResult() {
        saveResult.setValue(null);
    }

    @Nullable
    public AiProviderType getSavedProvider() {
        return ai.getProvider();
    }

    @Nullable
    public String getSavedBaseUrl() {
        return ai.getBaseUrl();
    }

    @Nullable
    public String getSavedModel() {
        return ai.getModel();
    }

    public boolean hasKey(@NonNull AiProviderType type) {
        return ai.hasKey(type);
    }

    public void save(@NonNull AiProviderType type, @NonNull String baseUrl, @NonNull String model,
                     @Nullable String apiKey) {
        GitForgeApp.container(getApplication()).getIo().execute(() -> {
            try {
                ai.saveConfig(type, baseUrl, model, apiKey);
                saveResult.postValue(new SaveResult(null));
            } catch (IllegalArgumentException e) {
                saveResult.postValue(new SaveResult(e.getMessage()));
            } catch (SecureStore.SecureStoreException e) {
                saveResult.postValue(new SaveResult(
                        "The key could not be stored securely on this device, so it was not saved."));
            }
        });
    }

    public void remove() {
        GitForgeApp.container(getApplication()).getIo().execute(ai::removeConfig);
    }
}
