package com.asifqureshi.gitforge.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.AppContainer;
import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.core.Resource;
import com.asifqureshi.gitforge.data.model.Page;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.data.model.UserProfile;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.ui.common.ErrorText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final int RECENT_COUNT = 5;

    private final AppContainer container;
    private final MutableLiveData<Resource<UserProfile>> profile = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Repo>>> recent = new MutableLiveData<>();
    private final LiveData<Integer> pushesToday;
    private final MediatorLiveData<String> successRate = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        container = GitForgeApp.container(application);

        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        pushesToday = container.getHistory().observeSuccessfulUploadsSince(midnight.getTimeInMillis());

        final LiveData<Integer> ok = container.getHistory().observeSuccessfulUploads();
        final LiveData<Integer> failed = container.getHistory().observeFailedUploads();
        Runnable recompute = () -> {
            Integer s = ok.getValue();
            Integer f = failed.getValue();
            if (s == null || f == null) {
                return;
            }
            int total = s + f;
            successRate.setValue(total == 0 ? null : Math.round(100f * s / total) + "%");
        };
        successRate.addSource(ok, v -> recompute.run());
        successRate.addSource(failed, v -> recompute.run());
        load();
    }

    public LiveData<Resource<UserProfile>> getProfile() {
        return profile;
    }

    public LiveData<Resource<List<Repo>>> getRecent() {
        return recent;
    }

    public LiveData<Integer> getPushesToday() {
        return pushesToday;
    }

    /** Percentage string, or null when there is no finished upload yet (the UI shows a dash). */
    public LiveData<String> getSuccessRate() {
        return successRate;
    }

    public void load() {
        profile.setValue(Resource.loading(null));
        recent.setValue(Resource.loading(null));
        container.getIo().execute(() -> {
            try {
                profile.postValue(Resource.success(container.getGitHub().getProfile()));
            } catch (ApiException e) {
                profile.postValue(Resource.error(ErrorText.describe(getApplication(), e), null));
            }
            try {
                Page<Repo> page = container.getGitHub().getRepos(1);
                List<Repo> top = new ArrayList<>(page.getItems().subList(0,
                        Math.min(RECENT_COUNT, page.getItems().size())));
                recent.postValue(Resource.success(top));
            } catch (ApiException e) {
                recent.postValue(Resource.error(ErrorText.describe(getApplication(), e), null));
            }
        });
    }

    public void select(@NonNull Repo repo) {
        container.setSelection(new Selection(repo, repo.getDefaultBranch()));
    }
}
