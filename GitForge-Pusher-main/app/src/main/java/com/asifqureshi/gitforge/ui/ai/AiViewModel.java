package com.asifqureshi.gitforge.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.AppContainer;
import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.core.Resource;
import com.asifqureshi.gitforge.data.remote.ai.AiException;
import com.asifqureshi.gitforge.domain.ai.AiPrompts;
import com.asifqureshi.gitforge.domain.ai.ProjectDigest;
import com.asifqureshi.gitforge.domain.project.ProjectFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AiViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Resource<String>> result = new MutableLiveData<>();
    private final MutableLiveData<String> digest = new MutableLiveData<>();
    private final MutableLiveData<String> digestError = new MutableLiveData<>();

    public AiViewModel(@NonNull Application application) {
        super(application);
        container = GitForgeApp.container(application);
    }

    public LiveData<Resource<String>> getResult() {
        return result;
    }

    public LiveData<String> getDigest() {
        return digest;
    }

    public LiveData<String> getDigestError() {
        return digestError;
    }

    public boolean isConfigured() {
        return container.getAi().isConfigured();
    }

    /** Host name of the configured endpoint, for the "this will be sent to ..." confirmation. */
    public String getProviderHost() {
        String url = container.getAi().getBaseUrl();
        if (url == null) {
            return "";
        }
        try {
            String host = new java.net.URI(url).getHost();
            return host != null ? host : url;
        } catch (java.net.URISyntaxException e) {
            return url;
        }
    }

    public void run(@NonNull AiPrompts.Task task, @NonNull String content) {
        result.setValue(Resource.loading(null));
        container.getIo().execute(() -> {
            try {
                result.postValue(Resource.success(container.getAi().run(task, content)));
            } catch (AiException e) {
                result.postValue(Resource.error(e.getMessage(), null));
            }
        });
    }

    /** Builds the project summary from files the user already staged (selected, non-secret text files only). */
    public void buildDigest(@NonNull List<ProjectFile> staged) {
        final List<ProjectFile> copy = new ArrayList<>(staged);
        container.getIo().execute(() -> {
            try {
                digest.postValue(ProjectDigest.build(copy));
            } catch (IOException e) {
                digestError.postValue("The staged files could not be read.");
            }
        });
    }
}
