package com.asifqureshi.gitforge.ui.repos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.AppContainer;
import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.data.model.Page;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.ui.common.ErrorText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RepoListViewModel extends AndroidViewModel {

    public enum Filter { ALL, PUBLIC, PRIVATE }

    /** Immutable snapshot of what the screen should show. */
    public static final class State {
        public final List<Repo> visible;
        public final boolean loading;
        public final boolean hasMore;
        @Nullable
        public final String error;
        public final boolean loadedAnything;

        State(List<Repo> visible, boolean loading, boolean hasMore, @Nullable String error, boolean loadedAnything) {
            this.visible = visible;
            this.loading = loading;
            this.hasMore = hasMore;
            this.error = error;
            this.loadedAnything = loadedAnything;
        }
    }

    /** Safety cap when searching: at most this many pages (100 repositories each) are fetched automatically. */
    private static final int MAX_AUTO_PAGES = 10;

    private final AppContainer container;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final List<Repo> all = new ArrayList<>();
    private int nextPage = 1;
    private boolean hasNext = true;
    private boolean loading;
    private boolean loadedAnything;
    private String error;
    private String query = "";
    private Filter filter = Filter.ALL;

    public RepoListViewModel(@NonNull Application application) {
        super(application);
        container = GitForgeApp.container(application);
        refresh();
    }

    public LiveData<State> getState() {
        return state;
    }

    public synchronized void refresh() {
        all.clear();
        nextPage = 1;
        hasNext = true;
        error = null;
        loadedAnything = false;
        loadNext();
    }

    public synchronized void loadMoreIfNeeded() {
        if (!loading && hasNext && error == null) {
            loadNext();
        }
    }

    public synchronized void retry() {
        error = null;
        loadNext();
    }

    public synchronized void setQuery(@NonNull String q) {
        query = q.trim().toLowerCase(Locale.ROOT);
        publish();
        if (!query.isEmpty() && hasNext && !loading && error == null) {
            loadNext();
        }
    }

    public synchronized void setFilter(@NonNull Filter f) {
        filter = f;
        publish();
    }

    public void select(@NonNull Repo repo) {
        container.setSelection(new Selection(repo, repo.getDefaultBranch()));
    }

    private void loadNext() {
        if (loading || !hasNext) {
            return;
        }
        loading = true;
        publish();
        final int page = nextPage;
        container.getIo().execute(() -> {
            Page<Repo> result = null;
            ApiException failure = null;
            try {
                result = container.getGitHub().getRepos(page);
            } catch (ApiException e) {
                failure = e;
            }
            synchronized (RepoListViewModel.this) {
                loading = false;
                if (result != null) {
                    all.addAll(result.getItems());
                    hasNext = result.hasNext();
                    nextPage = page + 1;
                    loadedAnything = true;
                } else {
                    error = ErrorText.describe(getApplication(), failure);
                }
                publish();
                // While a search is active, keep fetching pages so results are not limited to what is loaded.
                if (error == null && !query.isEmpty() && hasNext && nextPage <= MAX_AUTO_PAGES) {
                    loadNext();
                }
            }
        });
    }

    private void publish() {
        List<Repo> visible = new ArrayList<>();
        for (Repo r : all) {
            if (filter == Filter.PUBLIC && r.isPrivate()) {
                continue;
            }
            if (filter == Filter.PRIVATE && !r.isPrivate()) {
                continue;
            }
            if (!query.isEmpty() && !r.getFullName().toLowerCase(Locale.ROOT).contains(query)
                    && (r.getDescription() == null || !r.getDescription().toLowerCase(Locale.ROOT).contains(query))) {
                continue;
            }
            visible.add(r);
        }
        state.postValue(new State(visible, loading, hasNext, error, loadedAnything));
    }
}
