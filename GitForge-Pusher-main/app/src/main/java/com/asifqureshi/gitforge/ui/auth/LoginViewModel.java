package com.asifqureshi.gitforge.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.data.local.SecureStore;
import com.asifqureshi.gitforge.data.local.SettingsStore;
import com.asifqureshi.gitforge.data.remote.github.DeviceFlowSession;
import com.asifqureshi.gitforge.data.repository.AuthRepository;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.ui.common.ErrorText;

public class LoginViewModel extends AndroidViewModel {

    /** What the user must do on GitHub during the device flow. */
    public static final class DeviceInfo {
        public final String userCode;
        public final String verificationUri;

        DeviceInfo(String userCode, String verificationUri) {
            this.userCode = userCode;
            this.verificationUri = verificationUri;
        }
    }

    private final AuthRepository auth;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<DeviceInfo> device = new MutableLiveData<>();
    private DeviceFlowSession session;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.auth = GitForgeApp.container(application).getAuth();
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<DeviceInfo> getDevice() {
        return device;
    }

    public boolean isOAuthConfigured() {
        return auth.isOAuthConfigured();
    }

    public LiveData<String> getSessionNotice() {
        return auth.getSessionNotice();
    }

    public void signInWithToken(String token) {
        busy.setValue(true);
        error.setValue(null);
        GitForgeApp.container(getApplication()).getIo().execute(() -> {
            try {
                auth.signInWithToken(token, SettingsStore.AUTH_PAT);
            } catch (ApiException e) {
                error.postValue(ErrorText.describe(getApplication(), e));
            } catch (SecureStore.SecureStoreException e) {
                error.postValue("The token could not be stored securely on this device, so sign-in was cancelled.");
            } finally {
                busy.postValue(false);
            }
        });
    }

    public void startOAuth(boolean includePrivate) {
        cancelOAuth();
        error.setValue(null);
        busy.setValue(true);
        session = auth.startDeviceFlow(includePrivate, new DeviceFlowSession.Listener() {
            @Override
            public void onCode(@NonNull String userCode, @NonNull String verificationUri, int expiresInSeconds) {
                device.postValue(new DeviceInfo(userCode, verificationUri));
            }

            @Override
            public void onToken(@NonNull String accessToken) {
                try {
                    auth.signInWithToken(accessToken, SettingsStore.AUTH_OAUTH);
                } catch (ApiException e) {
                    error.postValue(ErrorText.describe(getApplication(), e));
                } catch (SecureStore.SecureStoreException e) {
                    error.postValue("The token could not be stored securely on this device, so sign-in was cancelled.");
                } finally {
                    device.postValue(null);
                    busy.postValue(false);
                }
            }

            @Override
            public void onFailure(@NonNull String message) {
                error.postValue(message);
                device.postValue(null);
                busy.postValue(false);
            }
        });
    }

    public void cancelOAuth() {
        if (session != null) {
            session.cancel();
            session = null;
        }
        device.postValue(null);
        busy.postValue(false);
    }

    @Override
    protected void onCleared() {
        if (session != null) {
            session.cancel();
        }
    }
}
