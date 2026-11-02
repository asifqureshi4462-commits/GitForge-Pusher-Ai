package com.asifqureshi.gitforge.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.BuildConfig;
import com.asifqureshi.gitforge.data.local.CredentialStore;
import com.asifqureshi.gitforge.data.local.SecureStore;
import com.asifqureshi.gitforge.data.local.SettingsStore;
import com.asifqureshi.gitforge.data.model.TokenCheck;
import com.asifqureshi.gitforge.data.model.UserProfile;
import com.asifqureshi.gitforge.data.remote.github.DeviceFlowApi;
import com.asifqureshi.gitforge.data.remote.github.DeviceFlowSession;
import com.asifqureshi.gitforge.data.remote.github.GitHubClient;
import com.asifqureshi.gitforge.domain.Validators;
import com.asifqureshi.gitforge.domain.github.ApiException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/** Sign-in state, token validation and storage, and the OAuth device flow. */
public final class AuthRepository {

    /** Minimal OAuth scopes. "public_repo" can only touch public repositories; "repo" includes private ones. */
    public static final String SCOPE_PUBLIC = "public_repo";
    public static final String SCOPE_PRIVATE = "repo";

    private final CredentialStore credentials;
    private final SettingsStore settings;
    private final GitHubClient github;
    private final DeviceFlowApi deviceFlowApi;
    private final ExecutorService io;
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>();
    private final MutableLiveData<String> sessionNotice = new MutableLiveData<>();
    private final List<Runnable> signOutHooks = new CopyOnWriteArrayList<>();

    public AuthRepository(@NonNull CredentialStore credentials, @NonNull SettingsStore settings,
                          @NonNull GitHubClient github, @NonNull DeviceFlowApi deviceFlowApi,
                          @NonNull ExecutorService io) {
        this.credentials = credentials;
        this.settings = settings;
        this.github = github;
        this.deviceFlowApi = deviceFlowApi;
        this.io = io;
        // Cheap, non-secret check at startup. If the token turns out to be unreadable or revoked, the first
        // API call fails with 401 and onTokenRejected() signs the user out with an explanation.
        this.signedIn.setValue(settings.getAccountLogin() != null);
        github.setUnauthorizedListener(this::onTokenRejected);
    }

    public LiveData<Boolean> getSignedIn() {
        return signedIn;
    }

    public boolean isSignedIn() {
        return Boolean.TRUE.equals(signedIn.getValue());
    }

    /** One-shot message to show on the sign-in screen (for example "session expired"). */
    public LiveData<String> getSessionNotice() {
        return sessionNotice;
    }

    public void clearSessionNotice() {
        sessionNotice.postValue(null);
    }

    public void addSignOutHook(@NonNull Runnable hook) {
        signOutHooks.add(hook);
    }

    /** True when the build contains a GitHub OAuth client id (device flow can be offered). */
    public boolean isOAuthConfigured() {
        return !BuildConfig.GITHUB_CLIENT_ID.isEmpty();
    }

    /**
     * Validates the token with GitHub, stores it encrypted and marks the user signed in. Blocking: call off the
     * main thread. The token is never logged and never appears in an exception message.
     */
    @NonNull
    public UserProfile signInWithToken(@Nullable String rawToken, @NonNull String method)
            throws ApiException, SecureStore.SecureStoreException {
        String token = Validators.normalizeToken(rawToken);
        String problem = Validators.validateToken(token);
        if (problem != null) {
            throw new ApiException(ApiException.Kind.VALIDATION, 0, problem);
        }
        TokenCheck check = github.checkToken(token);
        credentials.saveToken(token);
        UserProfile profile = check.getProfile();
        settings.setAccount(profile.getLogin(), profile.getName(), method, check.getScopes());
        sessionNotice.postValue(null);
        signedIn.postValue(true);
        return profile;
    }

    @NonNull
    public DeviceFlowSession startDeviceFlow(boolean includePrivate, @NonNull DeviceFlowSession.Listener listener) {
        DeviceFlowSession session = new DeviceFlowSession(deviceFlowApi, BuildConfig.GITHUB_CLIENT_ID,
                includePrivate ? SCOPE_PRIVATE : SCOPE_PUBLIC, io, listener);
        session.start();
        return session;
    }

    public void signOut() {
        credentials.clearToken();
        settings.clearAccount();
        for (Runnable hook : signOutHooks) {
            hook.run();
        }
        signedIn.postValue(false);
    }

    private void onTokenRejected() {
        if (isSignedIn()) {
            sessionNotice.postValue("Your GitHub session expired or the token was revoked. Sign in again.");
            signOut();
        }
    }
}
