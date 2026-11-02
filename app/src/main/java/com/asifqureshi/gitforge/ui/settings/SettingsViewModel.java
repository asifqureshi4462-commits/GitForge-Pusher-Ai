package com.asifqureshi.gitforge.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.AppContainer;
import com.asifqureshi.gitforge.GitForgeApp;

public class SettingsViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Long> cacheBytes = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        container = GitForgeApp.container(application);
        refreshCacheSize();
    }

    public LiveData<Long> getCacheBytes() {
        return cacheBytes;
    }

    public void refreshCacheSize() {
        container.getIo().execute(() -> cacheBytes.postValue(container.getStagingSizeBytes()));
    }

    public void clearCache() {
        container.getIo().execute(() -> {
            container.clearStaging();
            cacheBytes.postValue(container.getStagingSizeBytes());
        });
    }

    public void signOut() {
        container.getAuth().signOut();
    }
}
