package com.asifqureshi.gitforge;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.data.local.CredentialStore;
import com.asifqureshi.gitforge.data.local.SecureStore;
import com.asifqureshi.gitforge.data.local.SettingsStore;
import com.asifqureshi.gitforge.data.local.db.GitForgeDatabase;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.data.remote.ai.AiClient;
import com.asifqureshi.gitforge.data.remote.github.GitHubClient;
import com.asifqureshi.gitforge.data.remote.github.NetworkModule;
import com.asifqureshi.gitforge.data.repository.AiRepository;
import com.asifqureshi.gitforge.data.repository.AuthRepository;
import com.asifqureshi.gitforge.data.repository.GitHubRepository;
import com.asifqureshi.gitforge.data.repository.HistoryRepository;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manual dependency container (no DI framework; see docs/ARCHITECTURE.md). Every network and storage collaborator is
 * created exactly once here, so there is a single GitHub client and a single place that handles credentials.
 */
public final class AppContainer {

    private final Context appContext;
    private final ExecutorService io;
    private final SettingsStore settings;
    private final CredentialStore credentials;
    private final GitHubRepository gitHubRepository;
    private final AuthRepository auth;
    private final HistoryRepository history;
    private final AiRepository ai;
    private final MutableLiveData<Selection> selection = new MutableLiveData<>();

    AppContainer(@NonNull Context applicationContext) {
        this.appContext = applicationContext;
        final AtomicInteger counter = new AtomicInteger();
        this.io = Executors.newFixedThreadPool(4, runnable -> {
            Thread t = new Thread(runnable, "gitforge-io-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        String userAgent = "GitForgePusher/" + BuildConfig.VERSION_NAME;
        this.settings = new SettingsStore(applicationContext);
        this.credentials = new CredentialStore(new SecureStore(applicationContext));

        GitHubClient github = new GitHubClient(NetworkModule.createGitHubApi(credentials, userAgent));
        this.gitHubRepository = new GitHubRepository(github);
        this.auth = new AuthRepository(credentials, settings, github,
                NetworkModule.createDeviceFlowApi(userAgent), io);

        GitForgeDatabase database = GitForgeDatabase.create(applicationContext);
        this.history = new HistoryRepository(database.operationDao(), io);
        this.ai = new AiRepository(settings, credentials, new AiClient(NetworkModule.createAiHttpClient(userAgent)));

        // Signing out forgets the working selection and any staged project files.
        this.auth.addSignOutHook(() -> {
            selection.postValue(null);
            deleteRecursively(getStagingRoot());
        });
    }

    @NonNull
    public SettingsStore getSettings() {
        return settings;
    }

    @NonNull
    public AuthRepository getAuth() {
        return auth;
    }

    @NonNull
    public GitHubRepository getGitHub() {
        return gitHubRepository;
    }

    @NonNull
    public HistoryRepository getHistory() {
        return history;
    }

    @NonNull
    public AiRepository getAi() {
        return ai;
    }

    @NonNull
    public ExecutorService getIo() {
        return io;
    }

    @NonNull
    public LiveData<Selection> getSelection() {
        return selection;
    }

    @NonNull
    public Selection requireSelectionValue() {
        Selection s = selection.getValue();
        if (s == null) {
            throw new IllegalStateException("No repository selected");
        }
        return s;
    }

    public void setSelection(Selection value) {
        selection.postValue(value);
    }

    /** App-private folder that holds files staged for upload. Contents are disposable. */
    @NonNull
    public File getStagingRoot() {
        return new File(appContext.getCacheDir(), "staging");
    }

    public long getStagingSizeBytes() {
        return sizeOf(getStagingRoot());
    }

    public void clearStaging() {
        deleteRecursively(getStagingRoot());
    }

    private static long sizeOf(File f) {
        if (f.isFile()) {
            return f.length();
        }
        long total = 0;
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                total += sizeOf(c);
            }
        }
        return total;
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
