package com.asifqureshi.gitforge.ui.repos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.AppContainer;
import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.data.local.db.OperationEntity;
import com.asifqureshi.gitforge.data.model.Branch;
import com.asifqureshi.gitforge.data.model.Page;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.ui.common.ErrorText;

import java.util.ArrayList;
import java.util.List;

public class RepoDetailViewModel extends AndroidViewModel {

    public static final class State {
        public final List<Branch> branches;
        public final boolean loading;
        public final boolean hasMore;
        @Nullable
        public final String error;

        State(List<Branch> branches, boolean loading, boolean hasMore, @Nullable String error) {
            this.branches = branches;
            this.loading = loading;
            this.hasMore = hasMore;
            this.error = error;
        }
    }

    /** Result of a create-branch attempt, delivered once. */
    public static final class CreateResult {
        public final boolean success;
        public final String message;

        CreateResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final AppContainer container;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<CreateResult> createResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> creating = new MutableLiveData<>(false);
    private final List<Branch> branches = new ArrayList<>();
    private int nextPage = 1;
    private boolean hasNext = true;
    private boolean loading;
    private String error;

    public RepoDetailViewModel(@NonNull Application application) {
        super(application);
        container = GitForgeApp.container(application);
        refresh();
    }

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<CreateResult> getCreateResult() {
        return createResult;
    }

    public LiveData<Boolean> getCreating() {
        return creating;
    }

    public LiveData<Selection> getSelection() {
        return container.getSelection();
    }

    public synchronized void refresh() {
        branches.clear();
        nextPage = 1;
        hasNext = true;
        error = null;
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

    public void selectBranch(@NonNull String name) {
        Selection s = container.getSelection().getValue();
        if (s != null) {
            container.setSelection(s.withBranch(name));
        }
    }

    /** Creates {@code newName} from the currently selected branch (or the default branch). */
    public void createBranch(@NonNull String newName) {
        final Selection s = container.getSelection().getValue();
        if (s == null) {
            return;
        }
        final Repo repo = s.getRepo();
        final String from = s.getBranch() != null ? s.getBranch() : repo.getDefaultBranch();
        if (!repo.canPush() || from == null) {
            createResult.setValue(new CreateResult(false, getApplication().getString(
                    com.asifqureshi.gitforge.R.string.branch_create_not_allowed)));
            return;
        }
        creating.setValue(true);
        container.getIo().execute(() -> {
            OperationEntity op = new OperationEntity();
            op.repoFullName = repo.getFullName();
            op.branch = newName;
            op.operationType = OperationEntity.TYPE_CREATE_BRANCH;
            op.commitMessage = "Create branch " + newName + " from " + from;
            try {
                String sha = container.getGitHub().createBranch(repo, newName, from);
                op.result = OperationEntity.RESULT_SUCCESS;
                op.commitSha = sha;
                container.getHistory().record(op);
                container.setSelection(s.withBranch(newName));
                createResult.postValue(new CreateResult(true, newName));
                synchronized (RepoDetailViewModel.this) {
                    refresh();
                }
            } catch (ApiException e) {
                op.result = OperationEntity.RESULT_FAILED;
                op.errorSummary = e.getMessage();
                container.getHistory().record(op);
                String message = e.getKind() == ApiException.Kind.VALIDATION
                        ? getApplication().getString(com.asifqureshi.gitforge.R.string.branch_create_exists)
                        : ErrorText.describe(getApplication(), e);
                createResult.postValue(new CreateResult(false, message));
            } finally {
                creating.postValue(false);
            }
        });
    }

    private void loadNext() {
        final Selection s = container.getSelection().getValue();
        if (s == null || loading || !hasNext) {
            return;
        }
        loading = true;
        publish();
        final int page = nextPage;
        container.getIo().execute(() -> {
            Page<Branch> result = null;
            ApiException failure = null;
            try {
                result = container.getGitHub().getBranches(s.getRepo(), page);
            } catch (ApiException e) {
                failure = e;
            }
            synchronized (RepoDetailViewModel.this) {
                loading = false;
                if (result != null) {
                    branches.addAll(result.getItems());
                    hasNext = result.hasNext();
                    nextPage = page + 1;
                } else {
                    error = ErrorText.describe(getApplication(), failure);
                }
                publish();
            }
        });
    }

    private void publish() {
        state.postValue(new State(new ArrayList<>(branches), loading, hasNext, error));
    }
}
